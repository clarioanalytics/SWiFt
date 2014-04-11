package com.clario.swift;

import com.amazonaws.services.simpleworkflow.model.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.amazonaws.services.simpleworkflow.model.EventType.*;
import static com.clario.swift.SwiftUtil.defaultIfNull;
import static java.util.Arrays.asList;

/**
 * Each instance represents one activity
 *
 * @author George Coller
 */
public class ActivityDecisionStep extends DecisionStep {
    private String name;
    private String version;
    /**
     * Optional, allows additional input to be sent to activity.
     */
    // TODO: Implement a way of registering activities in SWF
    private String control = "";
    private String taskList = "default";
    private String heartBeatTimeoutTimeout;
    private String scheduleToCloseTimeout;
    private String scheduleToStartTimeout;
    private String startToCloseTimeout;

    public ActivityDecisionStep(String stepId, String name, String version) {
        super(stepId);
        this.name = name;
        this.version = version;
    }

    @Override
    public EventType getSuccessEventType() {
        return ActivityTaskCompleted;
    }

    @Override
    public List<EventType> getFailEventTypes() {
        return asList(ActivityTaskCompleted, ActivityTaskFailed, ActivityTaskTimedOut, ActivityTaskCanceled);
    }

    /**
     * Default implementation is to schedule this instances activity with all available inputs
     * serialized using this instance's {@link com.clario.swift.MapSerializer}.
     */
    @Override
    public List<Decision> decide() {
        String input = getIoSerializer().marshal(getInputs());
        return asList(createScheduleActivityDecision(input));
    }

    /**
     * Override activity's default heartbeat timeout.
     */
    public void setHeartBeatTimeout(TimeUnit unit, int duration) {
        heartBeatTimeoutTimeout = ((Long) unit.toSeconds(duration)).toString();
    }

    /**
     * Override activity's default schedule to close timeout.
     * <pre>
     * schedule ---> start ---> close
     * |_____________________________|
     * </pre>
     */
    public void setScheduleToCloseTimeout(TimeUnit unit, int duration) {
        this.scheduleToCloseTimeout = ((Long) unit.toSeconds(duration)).toString();
    }

    /**
     * Override activity's default schedule to close timeout.
     * <pre>
     * schedule ---> start ---> close
     * |_________________|
     * </pre>
     */
    public void setScheduleToStartTimeout(TimeUnit unit, int duration) {
        this.scheduleToStartTimeout = ((Long) unit.toSeconds(duration)).toString();
    }

    /**
     * Override activity's default schedule to close timeout.
     * <pre>
     * schedule ---> start ---> close
     *              |_______________|
     * </pre>
     */
    public void setStartToCloseTimeout(TimeUnit unit, int duration) {
        this.startToCloseTimeout = ((Long) unit.toSeconds(duration)).toString();
    }

    /**
     * Cancel activity decision for this step
     *
     * @return the decision
     * @see com.amazonaws.services.simpleworkflow.model.Decision#requestCancelActivityTaskDecisionAttributes
     */
    public Decision createCancelActivityDecision() {
        return new Decision().withDecisionType(DecisionType.RequestCancelActivityTask).withRequestCancelActivityTaskDecisionAttributes(new RequestCancelActivityTaskDecisionAttributes().withActivityId(getStepId()));
    }

    /**
     * Schedule activity decision for this step.
     *
     * @param input Input to activity
     *
     * @return the decision
     * @see com.amazonaws.services.simpleworkflow.model.Decision#scheduleActivityTaskDecisionAttributes for input size limitations
     */
    public Decision createScheduleActivityDecision(String input) {
        assert name != null;
        assert getStepId() != null;
        return new Decision()
            .withDecisionType(DecisionType.ScheduleActivityTask)
            .withScheduleActivityTaskDecisionAttributes(new ScheduleActivityTaskDecisionAttributes()
                .withActivityType(new ActivityType()
                    .withName(name)
                    .withVersion(defaultIfNull(version, "1.0")))
                .withActivityId(getStepId())
                .withTaskList(new TaskList()
                    .withName(defaultIfNull(taskList, "default")))
                .withInput(defaultIfNull(input, ""))
                .withControl(defaultIfNull(control, ""))
                .withHeartbeatTimeout(heartBeatTimeoutTimeout)
                .withScheduleToCloseTimeout(scheduleToCloseTimeout)
                .withScheduleToStartTimeout(scheduleToStartTimeout)
                .withStartToCloseTimeout(startToCloseTimeout));
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getControl() {
        return control;
    }

    public void setControl(String control) {
        this.control = control;
    }

    public String getTaskList() {
        return taskList;
    }

    public void setTaskList(String taskList) {
        this.taskList = taskList;
    }
}
