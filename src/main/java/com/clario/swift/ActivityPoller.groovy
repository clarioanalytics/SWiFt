package com.clario.swift

import com.amazonaws.services.simpleworkflow.model.*

import java.lang.reflect.Method

/**
 * Polls for activities on a given domain and task list and executes them.
 * @author George Coller
 */
public class ActivityPoller extends BasePoller {
    public static final int MAX_REASON_DETAILS_LENGTH = 32768
    Map<String, ActivityInvoker> activityMap = [:]
    MapSerializer ioSerializer = new MapSerializer()

    ActivityPoller(String id) {
        super(id)
    }

    void addActivities(Object... activities) {
        activities.each { activity ->
            activity.class.declaredMethods.each { Method m ->
                if (m != null && m.isAnnotationPresent(ActivityMethod.class)) {
                    ActivityMethod activityMethod = m.getAnnotation(ActivityMethod.class)
                    String key = makeKey(activityMethod.name(), activityMethod.version())
                    log.info "Register activity $key"
                    activityMap[key] = new ActivityInvoker(m, activity)
                }
            }
        }
    }

    @Override
    protected void poll() {
        def request = new PollForActivityTaskRequest()
                .withDomain(domain)
                .withTaskList(new TaskList().withName(taskList))
                .withIdentity(this.id)
        ActivityTask task = swf.pollForActivityTask(request)
        if (task.taskToken == null) {
            log.info("poll timeout")
            return
        }

        try {
            def key = makeKey(task.activityType.name, task.activityType.version)
            log.info("invoke '${task.activityId}': $key")
            if (activityMap.containsKey(key)) {
                Map<String, String> inputs = ioSerializer.unmarshal(task.input)
                Map<String, String> outputs = activityMap.get(key).invoke(task, inputs)
                String result = ioSerializer.marshal(outputs)

                def resp = new RespondActivityTaskCompletedRequest()
                        .withTaskToken(task.taskToken)
                        .withResult(result)
                log.info("completed '${task.activityId}': $key = '$outputs'")
                swf.respondActivityTaskCompleted(resp)
            } else {
                log.error("failed not registered '${task.activityId}'")
                respondActivityTaskFailed(task.taskToken, "activity not registered $task on $id")
            }
        } catch (Exception e) {
            log.error("failed '${task.activityId}'", e)
            respondActivityTaskFailed(task.taskToken, e.message, printStackTrace(e))
        }
    }

    void recordHeartbeat(String taskToken, String details) {
        try {
            def request = new RecordActivityTaskHeartbeatRequest()
                    .withTaskToken(taskToken)
                    .withDetails(details)
            swf.recordActivityTaskHeartbeat(request)
        } catch (e) {
            log.warn("Failed to record heartbeat: $taskToken, $details", e)
        }
    }

    void respondActivityTaskFailed(String taskToken, String reason, String details = "") {
        def failedRequest = new RespondActivityTaskFailedRequest()
                .withTaskToken(taskToken)
                .withReason(trimToMaxLength(reason))
                .withDetails(trimToMaxLength(details))
        log.warn("poll :$failedRequest")
        swf.respondActivityTaskFailed(failedRequest)
    }

    static String trimToMaxLength(String str) {
        if (str && str.length() > MAX_REASON_DETAILS_LENGTH) {
            return str.substring(0, MAX_REASON_DETAILS_LENGTH - 1)
        } else {
            str
        }
    }

    class ActivityInvoker implements ActivityContext {
        private Method method
        private Object instance
        Map<String, String> inputs = [:]
        Map<String, String> outputs = [:]
        ActivityTask task

        ActivityInvoker(Method method, Object instance) {
            this.method = method
            this.instance = instance
        }

        Map<String, String> invoke(ActivityTask task, Map<String, String> inputs) {
            try {
                this.task = task
                this.inputs = inputs
                outputs = [:]
                method.invoke(instance, this) as String
                outputs
            } catch (e) {
                throw new IllegalStateException("Failed to invoke with: ${task.activityId}: $inputs", e)
            }
        }

        @Override
        String getStepId() {
            task.activityId
        }

        @Override
        void recordHeartbeat(String details) {
            recordHeartbeat(task.taskToken, details)
        }

        @Override
        Map<String, String> getInputs() {
            return inputs
        }

        @Override
        Map<String, String> getOutputs() {
            return outputs
        }

        @Override
        void setOutput(Object value) {
            outputs[stepId] = value
        }
    }
}