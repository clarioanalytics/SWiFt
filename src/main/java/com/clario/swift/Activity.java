package com.clario.swift;

import com.amazonaws.services.simpleworkflow.model.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.amazonaws.services.simpleworkflow.model.EventType.*;
import static com.clario.swift.SwiftUtil.defaultIfNull;
import static java.util.Arrays.asList;

/**
 * Task that returns an SWF activity decision.
 *
 * @author George Coller
 */
public class Activity extends Task {
    private String name;
    private String version;
    // TODO: Implement a way of registering activities in SWF
    // Optional fields, allow additional input to be sent to activity.
    private String control = "";
    private String taskList = "default";
    private String heartBeatTimeoutTimeout;
    private String scheduleToCloseTimeout;
    private String scheduleToStartTimeout;
    private String startToCloseTimeout;

    public Activity(String id, String name, String version) {
        super(id);
        this.name = name;
        this.version = version;
    }

    @Override
    public EventType getScheduledEventType() {
        return ActivityTaskScheduled;
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
     * Cancel activity decision for this task
     *
     * @return the decision
     * @see com.amazonaws.services.simpleworkflow.model.Decision#requestCancelActivityTaskDecisionAttributes
     */
    public Decision createCancelActivityDecision() {
        return new Decision()
            .withDecisionType(DecisionType.RequestCancelActivityTask)
            .withRequestCancelActivityTaskDecisionAttributes(
                new RequestCancelActivityTaskDecisionAttributes().withActivityId(getId())
            );
    }

    /**
     * Schedule activity decision for this task.
     *
     * @param input Input to activity
     *
     * @return the decision
     * @see com.amazonaws.services.simpleworkflow.model.Decision#scheduleActivityTaskDecisionAttributes for input size limitations
     */
    public Decision createScheduleActivityDecision(String input) {
        assert name != null;
        assert getId() != null;
        return new Decision()
            .withDecisionType(DecisionType.ScheduleActivityTask)
            .withScheduleActivityTaskDecisionAttributes(new ScheduleActivityTaskDecisionAttributes()
                .withActivityType(new ActivityType()
                    .withName(name)
                    .withVersion(defaultIfNull(version, "1.0")))
                .withActivityId(getId())
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
