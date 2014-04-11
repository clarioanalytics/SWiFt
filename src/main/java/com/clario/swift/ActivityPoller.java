package com.clario.swift;

import com.amazonaws.services.simpleworkflow.model.*;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import static java.lang.String.valueOf;

/**
 * Polls for activities on a given domain and task list and executes them.
 *
 * @author George Coller
 */
public class ActivityPoller extends BasePoller {
    public static final int MAX_REASON_DETAILS_LENGTH = 32768;
    private Map<String, ActivityInvoker> activityMap = new LinkedHashMap<>();
    private MapSerializer ioSerializer = new MapSerializer();

    public ActivityPoller(String id) {
        super(id);
    }

    public void addActivities(Object... activities) {
        for (Object activity : activities) {
            for (Method m : activity.getClass().getDeclaredMethods()) {
                if (m != null && m.isAnnotationPresent(ActivityMethod.class)) {
                    ActivityMethod activityMethod = m.getAnnotation(ActivityMethod.class);
                    String key = BasePoller.makeKey(activityMethod.name(), activityMethod.version());
                    getLog().info("Register activity " + key);
                    activityMap.put(key, new ActivityInvoker(this, m, activity));
                }
            }
        }
    }

    @Override
    protected void poll() {
        PollForActivityTaskRequest request = new PollForActivityTaskRequest().withDomain(getDomain()).withTaskList(new TaskList().withName(getTaskList())).withIdentity(this.getId());
        final ActivityTask task = getSwf().pollForActivityTask(request);
        if (task.getTaskToken() == null) {
            getLog().info("poll timeout");
            return;

        }


        try {
            String key = BasePoller.makeKey(task.getActivityType().getName(), task.getActivityType().getVersion());
            getLog().info("invoke \'" + task.getActivityId() + "\': " + key);
            if (activityMap.containsKey(key)) {
                Map<String, String> inputs = ioSerializer.unmarshal(task.getInput());
                Map<String, String> outputs = activityMap.get(key).invoke(task, inputs);
                String result = ioSerializer.marshal(outputs);

                RespondActivityTaskCompletedRequest resp = new RespondActivityTaskCompletedRequest().withTaskToken(task.getTaskToken()).withResult(result);
                getLog().info("completed \'" + task.getActivityId() + "\': " + key + " = \'" + valueOf(outputs) + "\'");
                getSwf().respondActivityTaskCompleted(resp);
            } else {
                getLog().error("failed not registered \'" + task.getActivityId() + "\'");
                respondActivityTaskFailed(task.getTaskToken(), "activity not registered " + valueOf(task) + " on " + getId());
            }

        } catch (Exception e) {
            getLog().error("failed \'" + task.getActivityId() + "\'", e);
            respondActivityTaskFailed(task.getTaskToken(), e.getMessage(), BasePoller.printStackTrace(e));
        }

    }

    public void recordHeartbeat(String taskToken, String details) {
        try {
            RecordActivityTaskHeartbeatRequest request = new RecordActivityTaskHeartbeatRequest().withTaskToken(taskToken).withDetails(details);
            getSwf().recordActivityTaskHeartbeat(request);
        } catch (Throwable e) {
            getLog().warn("Failed to record heartbeat: " + taskToken + ", " + details, e);
        }

    }

    public void respondActivityTaskFailed(String taskToken, String reason, String details) {
        RespondActivityTaskFailedRequest failedRequest = new RespondActivityTaskFailedRequest().withTaskToken(taskToken).withReason(trimToMaxLength(reason)).withDetails(trimToMaxLength(details));
        getLog().warn("poll :" + valueOf(failedRequest));
        getSwf().respondActivityTaskFailed(failedRequest);
    }

    public void respondActivityTaskFailed(String taskToken, String reason) {
        respondActivityTaskFailed(taskToken, reason, "");
    }

    public static String trimToMaxLength(String str) {
        if (str != null && str.length() > MAX_REASON_DETAILS_LENGTH) {
            return str.substring(0, MAX_REASON_DETAILS_LENGTH - 1);
        } else {
            return str;
        }
    }

    public Map<String, ActivityInvoker> getActivityMap() {
        return activityMap;
    }

    public void setActivityMap(Map<String, ActivityInvoker> activityMap) {
        this.activityMap = activityMap;
    }

    public MapSerializer getIoSerializer() {
        return ioSerializer;
    }

    public void setIoSerializer(MapSerializer ioSerializer) {
        this.ioSerializer = ioSerializer;
    }

    public static class ActivityInvoker implements ActivityContext {
        private final ActivityPoller poller;
        private final Method method;
        private final Object instance;
        private Map<String, String> inputs = new LinkedHashMap<>();
        private Map<String, String> outputs = new LinkedHashMap<>();
        private ActivityTask task;

        public ActivityInvoker(ActivityPoller poller, Method method, Object instance) {
            this.poller = poller;
            this.method = method;
            this.instance = instance;
        }

        public Map<String, String> invoke(final ActivityTask task, Map<String, String> inputs) {
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

        @Override
        public String getStepId() {
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
        public Map<String, String> getOutputs() {
            return outputs;
        }

        @Override
        public void setOutput(String value) { outputs.put(getStepId(), value); }
    }
}
