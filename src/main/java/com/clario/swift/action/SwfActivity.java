package com.clario.swift.action;

import com.amazonaws.services.redshift.model.UnsupportedOptionException;
import com.amazonaws.services.simpleworkflow.model.*;

import java.util.concurrent.TimeUnit;

import static com.clario.swift.SwiftUtil.makeKey;
import static java.lang.String.format;

/**
 * Represents an SWF Activity task.
 *
 * @author George Coller
 */
public class SwfActivity extends SwfAction {
    private final String name;
    private final String version;
    private String input;
    private String control;
    private String heartBeatTimeoutTimeout;
    private String scheduleToCloseTimeout;
    private String scheduleToStartTimeout;
    private String startToCloseTimeout;


    public SwfActivity(String activityId, String name, String version) {
        super(activityId);
        this.name = name;
        this.version = version;
    }

    public SwfActivity withInput(String input) {
        this.input = input;
        return this;
    }

    public SwfAction withControl(String control) {
        this.control = control;
        return this;
    }

    /**
     * Override activity's default heartbeat timeout.
     *
     * @see ScheduleActivityTaskDecisionAttributes#heartbeatTimeout
     */
    public SwfAction withHeartBeatTimeoutTimeout(TimeUnit unit, long duration) {
        this.heartBeatTimeoutTimeout = Long.toString(unit.toSeconds(duration));
        return this;
    }

    /**
     * Override activity's default schedule to close timeout.
     * <pre>
     * schedule ---> start ---> close
     * |_____________________________|
     * </pre>
     *
     * @see ScheduleActivityTaskDecisionAttributes#scheduleToCloseTimeout
     */
    public SwfAction withScheduleToCloseTimeout(TimeUnit unit, long duration) {
        this.scheduleToCloseTimeout = Long.toString(unit.toSeconds(duration));
        return this;
    }

    /**
     * Override activity's default schedule to close timeout.
     * <pre>
     * schedule ---> start ---> close
     * |_________________|
     * </pre>
     *
     * @see ScheduleActivityTaskDecisionAttributes#scheduleToStartTimeout
     */
    public SwfAction withScheduleToStartTimeout(TimeUnit unit, long duration) {
        this.scheduleToStartTimeout = Long.toString(unit.toSeconds(duration));
        return this;
    }

    /**
     * Override activity's default start to close timeout.
     * <pre>
     * schedule ---> start ---> close
     *              |_______________|
     * </pre>
     *
     * @see ScheduleActivityTaskDecisionAttributes#startToCloseTimeout
     */
    public SwfActivity withStartToCloseTimeout(TimeUnit unit, long duration) {
        this.startToCloseTimeout = Long.toString(unit.toSeconds(duration));
        return this;
    }

    public String getOutput() {
        if (ActionState.finish_ok != getState()) {
            throw new UnsupportedOptionException(format("Result not available for activity %s with state %s", this, getState()));
        }
        return getCurrentActionHistoryEvent().getResult();
    }

    protected Decision createDecision() {
        return new Decision()
            .withDecisionType(DecisionType.ScheduleActivityTask)
            .withScheduleActivityTaskDecisionAttributes(new ScheduleActivityTaskDecisionAttributes()
                .withActivityType(new ActivityType()
                    .withName(name)
                    .withVersion(version))
                .withActivityId(id)
                .withTaskList(new TaskList()
                    .withName(workflow.getTaskList()))
                .withInput(input)
                .withControl(control)
                .withHeartbeatTimeout(heartBeatTimeoutTimeout)
                .withScheduleToCloseTimeout(scheduleToCloseTimeout)
                .withScheduleToStartTimeout(scheduleToStartTimeout)
                .withStartToCloseTimeout(startToCloseTimeout));
    }


    @Override
    public String toString() {
        return format("%s %s", id, makeKey(name, version));
    }
}
