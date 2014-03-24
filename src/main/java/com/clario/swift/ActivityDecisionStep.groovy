package com.clario.swift

import com.amazonaws.services.simpleworkflow.model.*

import java.util.concurrent.TimeUnit

import static com.amazonaws.services.simpleworkflow.model.EventType.*

/**
 * Each instance represents one activity
 * @author George Coller
 */
public class ActivityDecisionStep extends DecisionStep {

    String name
    String version
    /**
     * Optional, allows additional input to be sent to activity.
     */
    String control = ""
    String taskList = "default"
    String heartBeatTimeoutTimeout
    String scheduleToCloseTimeout
    String scheduleToStartTimeout
    String startToCloseTimeout

    ActivityDecisionStep(String stepId, String name, String version) {
        super(stepId)
        this.name = name
        this.version = version
    }

    @Override
    List<EventType> getFinalEventTypes() {
        return [ActivityTaskCompleted, ActivityTaskFailed, ActivityTaskTimedOut, ActivityTaskCanceled]
    }

    @Override
    Map<String, String> getOutput() {
        StepEvent stepEvent = stepEvents()?.first()
        if (!(stepEvent && stepEvent.type == ActivityTaskCompleted)) {
            throw new IllegalStateException("ActivityTaskCompleted not available")
        }
        ioSerializer.unmarshal(stepEvent.historyEvent.activityTaskCompletedEventAttributes.result)
    }

    /**
     * Default implementation is to schedule this instances activity with all available inputs
     * serialized using this instance's {@link MapSerializer}.
     */
    @Override
    List<Decision> decide() {
        def input = ioSerializer.marshal(inputs)
        [createScheduleActivityDecision(input)]
    }

    // Helpers

    /**
     * Override activity's default heartbeat timeout.
     */
    void setHeartBeatTimeout(TimeUnit unit, int duration) {
        heartBeatTimeoutTimeout = unit.toSeconds(duration).toString();
    }

    /**
     * Override activity's default schedule to close timeout.
     * <pre>
     * schedule ---> start ---> close
     * |_____________________________|
     * </pre>
     */
    void setScheduleToCloseTimeout(TimeUnit unit, int duration) {
        this.scheduleToCloseTimeout = unit.toSeconds(duration).toString();
    }

    /**
     * Override activity's default schedule to close timeout.
     * <pre>
     * schedule ---> start ---> close
     * |_________________|
     * </pre>
     */
    void setScheduleToStartTimeout(TimeUnit unit, int duration) {
        this.scheduleToStartTimeout = unit.toSeconds(duration).toString();
    }

    /**
     * Override activity's default schedule to close timeout.
     * <pre>
     * schedule ---> start ---> close
     *              |_______________|
     * </pre>
     */
    void setStartToCloseTimeout(TimeUnit unit, int duration) {
        this.startToCloseTimeout = unit.toSeconds(duration).toString();
    }

    /**
     * Cancel activity decision for this step
     * @return the decision
     * @see Decision#requestCancelActivityTaskDecisionAttributes
     */
    Decision createCancelActivityDecision() {
        new Decision()
                .withDecisionType(DecisionType.RequestCancelActivityTask)
                .withRequestCancelActivityTaskDecisionAttributes(
                new RequestCancelActivityTaskDecisionAttributes()
                        .withActivityId(stepId))
    }

    /**
     * Schedule activity decision for this step.
     * @param input Input to activity
     * @return the decision
     * @see Decision#scheduleActivityTaskDecisionAttributes for input size limitations
     */
    Decision createScheduleActivityDecision(String input) {
        assert name != null
        assert stepId != null

        new Decision()
                .withDecisionType(DecisionType.ScheduleActivityTask)
                .withScheduleActivityTaskDecisionAttributes(
                new ScheduleActivityTaskDecisionAttributes()
                        .withActivityType(new ActivityType().withName(name).withVersion(version ?: "1.0"))
                        .withActivityId(stepId)
                        .withTaskList(new TaskList().withName(taskList ?: "default"))
                        .withInput(input ?: "")
                        .withControl(control ?: "")
                        .withHeartbeatTimeout(heartBeatTimeoutTimeout)
                        .withScheduleToCloseTimeout(scheduleToCloseTimeout)
                        .withScheduleToStartTimeout(scheduleToStartTimeout)
                        .withStartToCloseTimeout(startToCloseTimeout))
    }
}