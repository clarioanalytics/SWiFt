package com.clario.swift;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.model.ActivityTask;
import com.amazonaws.services.simpleworkflow.model.TypeAlreadyExistsException;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.clario.swift.SwiftUtil.*;
import static java.lang.String.format;

/**
 * Polls for activities on a given domain and task list and executes them.
 *
 * @author George Coller
 */
public class ActivityPoller extends BasePoller {
    private Map<String, ActivityInvoker> activityMap = new LinkedHashMap<>();

    public ActivityPoller(String id, String domain, String taskList) {
        super(id, domain, taskList);
    }

    /**
     * Call to register all added activities on this poller's domain and task list.
     * <p/>
     * Method is idempotent and will log warning messages for any activities that were already registered.
     *
     * @see ActivityMethod
     * @see AmazonSimpleWorkflow#registerActivityType
     */
    public void registerSwfActivities() {
        for (ActivityInvoker invoker : activityMap.values()) {
            ActivityMethod method = invoker.getActivityMethod();
            try {
                log.info(format("Register activity '%s' '%s'", method.name(), method.version()));
                swf.registerActivityType(createRegisterActivityType(
                    domain,
                    taskList,
                    method.name(),
                    method.version(),
                    method.description(),
                    method.heartbeatTimeout(),
                    method.startToCloseTimeout(),
                    method.scheduleToStartTimeout(),
                    method.scheduleToCloseTimeout()
                ));
                log.info(format("Register activity succeeded '%s' '%s'", method.name(), method.version()));
            } catch (TypeAlreadyExistsException e) {
                log.warn(format("Activity already registered '%s' '%s'", method.name(), method.version()));
            } catch (Exception e) {
                log.warn(format("Failed to register activity '%s' '%s'", method.name(), method.version()));
            }
        }
    }


    /**
     * Add one or more objects having methods annotated with {@link ActivityMethod}.
     *
     * @param annotatedObjects annotated objects
     */
    public void addActivities(Object... annotatedObjects) {
        for (Object object : annotatedObjects) {
            for (Method method : object.getClass().getDeclaredMethods()) {
                if (method != null && method.isAnnotationPresent(ActivityMethod.class)) {
                    ActivityMethod activityMethod = method.getAnnotation(ActivityMethod.class);
                    String key = makeKey(activityMethod.name(), activityMethod.version());
                    log.info("Added activity " + key);
                    activityMap.put(key, new ActivityInvoker(this, method, object));
                }
            }
        }
    }

    @Override
    protected void poll() {
        ActivityTask task = swf.pollForActivityTask(createPollForActivityTask(domain, taskList, getId()));
        if (task.getTaskToken() == null) {
            log.info("poll timeout");
            return;
        }

        String input = task.getInput();
        String key = makeKey(task.getActivityType().getName(), task.getActivityType().getVersion());
        try {
            if (log.isInfoEnabled()) {
                log.info(format("invoke %s %s(%s)", task.getActivityId(), key, input));
            }
            if (activityMap.containsKey(key)) {
                String result = activityMap.get(key).invoke(task, input);

                if (log.isInfoEnabled()) {
                    log.info(format("completed %s %s(%s)=%s", task.getActivityId(), key, input, result));
                }
                swf.respondActivityTaskCompleted(createRespondActivityCompleted(task, result));
            } else {
                String format = format("Activity '%s' not registered on poller %s", task.getActivityId(), getId());
                log.error(format);
                swf.respondActivityTaskFailed(
                    createRespondActivityTaskFailed(task.getTaskToken(), format, null)
                );
            }
        } catch (Exception e) {
            log.error(format("failed %s %s(%s)", task.getActivityId(), key, input));
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

    static class ActivityInvoker implements ActivityContext {
        private final ActivityPoller poller;
        private final Method method;
        private final Object instance;
        private String input;
        private String output;
        private ActivityTask task;

        ActivityInvoker(ActivityPoller poller, Method method, Object instance) {
            this.poller = poller;
            this.method = method;
            this.instance = instance;
        }

        String invoke(final ActivityTask task, String input) {
            try {
                this.task = task;
                this.input = input;
                method.invoke(instance, this);
                return output;
            } catch (Throwable e) {
                throw new IllegalStateException(format("Failed to invoke with: %s: %s", task.getActivityId(), input), e);
            }
        }

        ActivityMethod getActivityMethod() {
            return method.getAnnotation(ActivityMethod.class);
        }

        @Override
        public String getId() {
            return task.getActivityId();
        }

        @Override
        public void recordHeartbeat(String details) {
            poller.recordHeartbeat(task.getTaskToken(), details);
        }

        public String getInput() {
            return input;
        }

        public void setInput(String input) {
            this.input = input;
        }

        public String getOutput() {
            return output;
        }

        public void setOutput(String output) {
            this.output = output;
        }
    }
}
