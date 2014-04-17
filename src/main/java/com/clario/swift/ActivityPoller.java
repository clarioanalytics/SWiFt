package com.clario.swift;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.model.ActivityTask;
import com.amazonaws.services.simpleworkflow.model.TypeAlreadyExistsException;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import static com.clario.swift.SwiftUtil.*;
import static java.lang.String.format;
import static java.lang.String.valueOf;

/**
 * Polls for activities on a given domain and task list and executes them.
 *
 * @author George Coller
 */
public class ActivityPoller extends BasePoller {
    private Map<String, ActivityInvoker> activityMap = new LinkedHashMap<>();
    private MapSerializer ioSerializer = new MapSerializer();

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
    public void registerSimpleWorkflowActivities() {
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

        try {
            String key = makeKey(task.getActivityType().getName(), task.getActivityType().getVersion());
            log.info(format("invoke '%s': %s", task.getActivityId(), key));
            if (activityMap.containsKey(key)) {
                Map<String, String> inputs = ioSerializer.unmarshal(task.getInput());
                Map<String, String> outputs = activityMap.get(key).invoke(task, inputs);
                String result = ioSerializer.marshal(outputs);

                if (log.isInfoEnabled()) {
                    String outputString = join(joinEntries(outputs, " -> "), ", ");
                    log.info(format("completed '%s': %s = '%s'", task.getActivityId(), key, outputString));
                }
                swf.respondActivityTaskCompleted(createRespondActivityCompleted(task, result));
            } else {
                log.error("failed not registered \'" + task.getActivityId() + "\'");
                swf.respondActivityTaskFailed(
                    createRespondActivityTaskFailed(task.getTaskToken(), "activity not registered " + valueOf(task) + " on " + getId(), null)
                );
            }

        } catch (Exception e) {
            log.error("failed \'" + task.getActivityId() + "\'", e);
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

    public void setIoSerializer(MapSerializer ioSerializer) {
        this.ioSerializer = ioSerializer;
    }

    static class ActivityInvoker implements ActivityContext {
        private final ActivityPoller poller;
        private final Method method;
        private final Object instance;
        private Map<String, String> inputs = new LinkedHashMap<>();
        private Map<String, String> outputs = new LinkedHashMap<>();
        private ActivityTask task;

        ActivityInvoker(ActivityPoller poller, Method method, Object instance) {
            this.poller = poller;
            this.method = method;
            this.instance = instance;
        }

        Map<String, String> invoke(final ActivityTask task, Map<String, String> inputs) {
            try {
                this.task = task;
                this.inputs = inputs;
                outputs = new LinkedHashMap<>();
                method.invoke(instance, this);
                return outputs;
            } catch (Throwable e) {
                throw new IllegalStateException("Failed to invoke with: " + task.getActivityId() + ": " + valueOf(inputs), e);
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

        @Override
        public Map<String, String> getInputs() {
            return inputs;
        }

        @Override
        public void setOutput(String value) { outputs.put(getId(), value); }

    }
}
