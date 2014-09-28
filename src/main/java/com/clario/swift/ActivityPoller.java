package com.clario.swift;

import com.amazonaws.services.simpleworkflow.model.*;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.clario.swift.SwiftUtil.*;
import static java.lang.String.format;

/**
 * Polls for activities on a given domain and task list and executes them.
 * <p/>
 * Implements {@link Runnable} so that multiple instances of this class can be
 * scheduled to handle higher levels of activity tasks.
 * <p/>
 * Since this class is single-threaded it will be tied-up while the activity is processing so scale
 * the size of the activity polling pool appropriately if you have many long-running activities.
 *
 * @author George Coller
 * @see BasePoller
 * @see com.clario.swift.examples.ActivityPollerPool StartActivityPollers for example usage.
 */
public class ActivityPoller extends BasePoller {
    private final Map<String, ActivityInvoker> activityMap = new LinkedHashMap<String, ActivityInvoker>();

    /**
     * @param id unique id for poller used for logging and recording in SWF
     * @param domain SWF domain to poll
     * @param taskList SWF taskList to filter on
     */
    public ActivityPoller(String id, String domain, String taskList) {
        super(id, domain, taskList);
    }

    /**
     * Register activities added to this poller on Amazon SWF with this instance's domain and task list.
     * {@link TypeAlreadyExistsException} are ignored making this method idempotent.
     *
     * @see ActivityMethod
     */
    public void registerSwfActivities() {
        for (ActivityInvoker invoker : activityMap.values()) {
            ActivityMethod method = invoker.getActivityMethod();
            try {
                log.info(format("Register activity '%s' '%s'", method.name(), method.version()));
                swf.registerActivityType(createRegisterActivityType(domain, taskList, method));
                log.info(format("Register activity succeeded '%s' '%s'", method.name(), method.version()));
            } catch (TypeAlreadyExistsException e) {
                log.warn(format("Activity already registered '%s' '%s'", method.name(), method.version()));
            } catch (Throwable t) {
                String format = format("Register activity failed '%s' '%s'", method.name(), method.version());
                log.error(format, t);
                throw new IllegalStateException(format, t);
            }
        }
    }

    /**
     * Add objects with one or more methods annotated with {@link ActivityMethod}
     * mirroring Activity Types registered on SWF with this poller's domain and task list.
     *
     * @param annotatedObjects objects with one or more methods annotated with {@link ActivityMethod}
     */
    public void addActivities(Object... annotatedObjects) {
        for (Object object : annotatedObjects) {
            for (Method method : object.getClass().getDeclaredMethods()) {
                if (method != null && method.isAnnotationPresent(ActivityMethod.class)) {
                    ActivityMethod activityMethod = method.getAnnotation(ActivityMethod.class);
                    String key = makeKey(activityMethod.name(), activityMethod.version());
                    log.info(format("add activity '%s'", key));
                    activityMap.put(key, new ActivityInvoker(this, method, object));
                }
            }
        }
    }

    /**
     * Each call performs a long polling or the next activity task from SWF and then calls
     * the matching registered {@link ActivityMethod} method to perform the task.
     * <p/>
     * <ul>
     * <li>Methods that succeed will cause a {@link RespondActivityTaskCompletedRequest} to be returned.</li>
     * <li>Methods that throw methods will cause a {@link RespondActivityTaskFailedRequest} to be returned.</li>
     * <li>Methods may issue zero or more {@link RecordActivityTaskHeartbeatRequest} calls while processing</li>
     * </ul>
     *
     * @see #addActivities(Object...)
     */
    @Override
    protected void poll() {
        ActivityTask task = swf.pollForActivityTask(createPollForActivityTask(domain, taskList, getId()));
        if (task.getTaskToken() == null) {
            if (isLogTimeout()) { log.info("poll timeout"); }
            return;
        }

        String input = task.getInput();
        String key = makeKey(task.getActivityType().getName(), task.getActivityType().getVersion());
        try {
            log.debug("start: {}", task);
            if (activityMap.containsKey(key)) {
                String result = activityMap.get(key).invoke(task);
                log.info("'{}' '{}' '{}' -> '{}'", task.getActivityId(), key, input, result);
                swf.respondActivityTaskCompleted(createRespondActivityCompleted(task, result));
            } else {
                String format = format("Activity '%s' not registered on poller %s", task, getId());
                log.error(format);
                swf.respondActivityTaskFailed(
                    createRespondActivityTaskFailed(task.getTaskToken(), format, null)
                );
            }
        } catch (Exception e) {
            log.error("'{}' '{}' '{}'", task.getActivityId(), key, input);
            swf.respondActivityTaskFailed(
                createRespondActivityTaskFailed(task.getTaskToken(), e.getMessage(), printStackTrace(e))
            );
        }
    }

    /**
     * Record a heartbeat on SWF.
     *
     * @param taskToken identifies the task recording the heartbeat
     * @param details information to be recorded
     */
    protected void recordHeartbeat(String taskToken, String details) {
        try {
            swf.recordActivityTaskHeartbeat(createRecordActivityTaskHeartbeat(taskToken, details));
        } catch (Throwable e) {
            log.warn("Failed to record heartbeat: " + taskToken + ", " + details, e);
        }

    }

    /**
     * Wraps a single method annotated with {@link ActivityMethod} and is registered on
     * the activity map.
     *
     * This class acts as the {@link ActivityContext} passed when calling an {@link ActivityMethod}.
     *
     * @see ActivityContext
     */
    static class ActivityInvoker implements ActivityContext {
        private final ActivityPoller poller;
        private final Method method;
        private final Object instance;
        private String input;
        private ActivityTask task;

        ActivityInvoker(ActivityPoller poller, Method method, Object instance) {
            this.poller = poller;
            this.method = method;
            this.instance = instance;
        }

        String invoke(final ActivityTask task) {
            String name = task.getActivityType() == null ? "unknown" : task.getActivityType().getName();
            try {
                this.task = task;
                this.input = task.getInput();
                Object result = method.invoke(instance, this);
                if (result == null) {
                    return null;
                } else {
                    String resultString = result.toString();
                    if (resultString.length() > MAX_RESULT_LENGTH) {
                        poller.log.warn(format("Activity '%s' '%s' returned result string longer than allowed %d characters, was trimmed\nresult was \"%s\"", task.getActivityId(), name, MAX_RESULT_LENGTH, resultString));
                    }
                    return trimToMaxLength(resultString, MAX_RESULT_LENGTH);
                }
            } catch (Throwable e) {
                throw new IllegalStateException(format("error: '%s' '%s' '%s'", task.getActivityId(), name, task.getInput()), e);
            }
        }

        ActivityMethod getActivityMethod() {
            return method.getAnnotation(ActivityMethod.class);
        }

        public String getActionId() {
            return task.getActivityId();
        }

        public void recordHeartbeat(String details) {
            poller.recordHeartbeat(task.getTaskToken(), details);
        }

        public String getInput() {
            return input;
        }
    }

    public static RegisterActivityTypeRequest createRegisterActivityType(String domain, String taskList, ActivityMethod method) {
        return new RegisterActivityTypeRequest()
            .withDomain(domain)
            .withDefaultTaskList(new TaskList().withName(taskList))
            .withName(method.name())
            .withVersion(method.version())
            .withDescription(defaultIfEmpty(method.description(), null))
            .withDefaultTaskHeartbeatTimeout(defaultIfEmpty(method.heartbeatTimeout(), SWF_TIMEOUT_NONE))
            .withDefaultTaskStartToCloseTimeout(defaultIfEmpty(method.startToCloseTimeout(), SWF_TIMEOUT_NONE))
            .withDefaultTaskScheduleToStartTimeout(defaultIfEmpty(method.scheduleToStartTimeout(), SWF_TIMEOUT_NONE))
            .withDefaultTaskScheduleToCloseTimeout(defaultIfEmpty(method.scheduleToCloseTimeout(), SWF_TIMEOUT_NONE));
    }

    public static PollForActivityTaskRequest createPollForActivityTask(String domain, String taskList, String id) {
        return new PollForActivityTaskRequest()
            .withDomain(domain)
            .withTaskList(new TaskList()
                .withName(taskList))
            .withIdentity(id);
    }

    public static RecordActivityTaskHeartbeatRequest createRecordActivityTaskHeartbeat(String taskToken, String details) {
        return new RecordActivityTaskHeartbeatRequest()
            .withTaskToken(taskToken)
            .withDetails(trimToMaxLength(details, MAX_DETAILS_LENGTH));
    }

    public static RespondActivityTaskFailedRequest createRespondActivityTaskFailed(String taskToken, String reason, String details) {
        return new RespondActivityTaskFailedRequest()
            .withTaskToken(taskToken)
            .withReason(trimToMaxLength(reason, MAX_REASON_LENGTH))
            .withDetails(trimToMaxLength(details, MAX_DETAILS_LENGTH));
    }

    public static RespondActivityTaskCompletedRequest createRespondActivityCompleted(ActivityTask task, String result) {
        return new RespondActivityTaskCompletedRequest()
            .withTaskToken(task.getTaskToken())
            .withResult(trimToMaxLength(result, MAX_RESULT_LENGTH));
    }

}
