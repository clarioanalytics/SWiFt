package com.clario.swift;

import com.amazonaws.services.redshift.model.UnsupportedOptionException;
import com.amazonaws.services.simpleworkflow.model.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

/**
 * @author George Coller
 */
public class SwiftActivity {
    private final String activityId;
    private final String name;
    private final String version;
    private String control;
    private String heartBeatTimeoutTimeout;
    private String scheduleToCloseTimeout;
    private String scheduleToStartTimeout;
    private String startToCloseTimeout;
    private boolean failWorkflowOnError = true;
    private Workflow workflow;


    public SwiftActivity(String activityId, String name, String version) {
        this.activityId = activityId;
        this.name = name;
        this.version = version;
    }

    public SwiftActivity withControl(String control) {
        this.control = control;
        return this;
    }

    /**
     * Override activity's default heartbeat timeout.
     */
    public SwiftActivity withHeartBeatTimeoutTimeout(TimeUnit unit, long duration) {
        this.heartBeatTimeoutTimeout = Long.toString(unit.toMillis(duration));
        return this;
    }

    /**
     * Override activity's default schedule to close timeout.
     * <pre>
     * schedule ---> start ---> close
     * |_____________________________|
     * </pre>
     */
    public SwiftActivity withScheduleToCloseTimeout(TimeUnit unit, long duration) {
        this.scheduleToCloseTimeout = Long.toString(unit.toMillis(duration));
        return this;
    }

    /**
     * Override activity's default schedule to close timeout.
     * <pre>
     * schedule ---> start ---> close
     * |_________________|
     * </pre>
     */
    public SwiftActivity withScheduleToStartTimeout(TimeUnit unit, long duration) {
        this.scheduleToStartTimeout = Long.toString(unit.toMillis(duration));
        return this;
    }

    /**
     * Override activity's default start to close timeout.
     * <pre>
     * schedule ---> start ---> close
     *              |_______________|
     * </pre>
     */
    public SwiftActivity withStartToCloseTimeout(TimeUnit unit, long duration) {
        this.startToCloseTimeout = Long.toString(unit.toMillis(duration));
        return this;
    }

    public SwiftActivity withFailWorkflowOnError(boolean value) {
        failWorkflowOnError = value;
        return this;
    }

    public Decision createDecision(String input) {
        return new Decision()
            .withDecisionType(DecisionType.ScheduleActivityTask)
            .withScheduleActivityTaskDecisionAttributes(new ScheduleActivityTaskDecisionAttributes()
                .withActivityType(new ActivityType()
                    .withName(name)
                    .withVersion(version))
                .withActivityId(activityId)
                .withTaskList(new TaskList()
                    .withName(workflow.getTaskList()))
                .withInput(input)
                .withControl(control)
                .withHeartbeatTimeout(heartBeatTimeoutTimeout)
                .withScheduleToCloseTimeout(scheduleToCloseTimeout)
                .withScheduleToStartTimeout(scheduleToStartTimeout)
                .withStartToCloseTimeout(startToCloseTimeout));
    }

    public boolean decided(List<Decision> decisions, String input) {
        if (workflow == null) {
            throw new AssertionError(format("Workflow not set on activity %s", this));
        }
        switch (getTaskState()) {
            case initial:
                decisions.add(createDecision(input));
                return false;
            case decided:
                return false;
            case finish_ok:
                return true;
            case finish_error:
                if (failWorkflowOnError) {
                    String reason = format("Activity '%s' error", activityId);
                    Decision fail = SwiftUtil.createFailWorkflowExecutionDecision(reason, null);
                    decisions.add(fail);
                    return false;
                } else {
                    return true;
                }
            default:
                throw new IllegalStateException("Unknown task state:" + getTaskState());
        }
    }

    public boolean isFinished() {
        return TaskState.finish_ok == getTaskState() || TaskState.finish_error == getTaskState();
    }

    public String getOutput() {
        if (TaskState.finish_ok != getTaskState()) {
            throw new UnsupportedOptionException("Result not available for activity with state: " + getTaskState());
        }
        return getCurrentTaskEvent().getResult();
    }

    public void setWorkflow(Workflow workflow) {
        this.workflow = workflow;
    }

    public TaskState getTaskState() {
        TaskEvent taskEvent = getCurrentTaskEvent();
        return taskEvent == null ? TaskState.initial : taskEvent.getTaskState();
    }

    public List<TaskEvent> getTaskEvents() {
        return workflow.getHistoryInspector().taskEvents(activityId);
    }

    public TaskEvent getCurrentTaskEvent() {
        List<TaskEvent> events = getTaskEvents();
        return events.isEmpty() ? null : events.get(0);
    }

    @Override
    public boolean equals(Object o) {
        return this == o || !(o == null || o instanceof SwiftActivity) && activityId.equals(((SwiftActivity) o).activityId);
    }

    @Override
    public int hashCode() {
        return activityId.hashCode();
    }

    @Override
    public String toString() {
        return format("Activity '%s' '%s'}", activityId, SwiftUtil.makeKey(name, version));
    }
}
