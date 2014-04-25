package com.clario.swift.action;

import com.amazonaws.services.redshift.model.UnsupportedOptionException;
import com.amazonaws.services.simpleworkflow.model.*;

import java.util.concurrent.TimeUnit;

import static com.clario.swift.SwiftUtil.calcTimeoutString;
import static com.clario.swift.SwiftUtil.makeKey;
import static java.lang.String.format;

/**
 * Maps to a registered (or to-be registered) SWF Activity.
 *
 * @author George Coller
 */
public class ActivityAction extends Action<ActivityAction> {
    private String name;
    private String version;
    private String input;
    private String control;
    private String heartBeatTimeoutTimeout;
    private String scheduleToCloseTimeout;
    private String scheduleToStartTimeout;
    private String startToCloseTimeout;


    /**
     * Useful for creating an instance in a workflow but deferring which
     * specific SWF Activity will be initiated until run time;
     *
     * @param actionId workflow-unique identifier.
     *
     * @see #withNameVersion
     */
    public ActivityAction(String actionId) {
        super(actionId);
    }

    /**
     * Construct an action mapped to a registered SWF Activity.
     * Each SWF Activity task is identified by the combination of name and version.
     *
     * @param actionId workflow-unique identifier.
     * @param name registered name
     * @param version registered version
     */
    public ActivityAction(String actionId, String name, String version) {
        super(actionId);
        withNameVersion(name, version);
    }

    /**
     * Registered SWF Activities are uniquely identified by the combination of name and version
     * so both are required.
     */
    public ActivityAction withNameVersion(String name, String version) {
        this.name = name;
        this.version = version;
        return this;
    }

    /**
     * @see ScheduleActivityTaskDecisionAttributes#input
     */
    public ActivityAction withInput(String input) {
        this.input = input;
        return this;
    }

    /**
     * @see ScheduleActivityTaskDecisionAttributes#control
     */
    public ActivityAction withControl(String control) {
        this.control = control;
        return this;
    }

    /**
     * Override activity's default heartbeat timeout.
     * Pass null unit or duration &lt;= 0 for a timeout of NONE.
     *
     * @see ScheduleActivityTaskDecisionAttributes#heartbeatTimeout
     */
    public ActivityAction withHeartBeatTimeoutTimeout(TimeUnit unit, long duration) {
        this.heartBeatTimeoutTimeout = calcTimeoutString(unit, duration);
        return this;
    }

    /**
     * Override activity's default schedule to close timeout.
     * <pre>
     * schedule ---> start ---> close
     * |_____________________________|
     * </pre>
     * Pass null unit or duration &lt;= 0 for a timeout of NONE.
     *
     * @see ScheduleActivityTaskDecisionAttributes#scheduleToCloseTimeout
     */
    public ActivityAction withScheduleToCloseTimeout(TimeUnit unit, long duration) {
        this.scheduleToCloseTimeout = calcTimeoutString(unit, duration);
        return this;
    }

    /**
     * Override activity's default schedule to close timeout.
     * <pre>
     * schedule ---> start ---> close
     * |_________________|
     * </pre>
     * Pass null unit or duration &lt;= 0 for a timeout of NONE.
     *
     * @see ScheduleActivityTaskDecisionAttributes#scheduleToStartTimeout
     */
    public ActivityAction withScheduleToStartTimeout(TimeUnit unit, long duration) {
        this.scheduleToStartTimeout = calcTimeoutString(unit, duration);
        return this;
    }

    /**
     * Override activity's default start to close timeout.
     * <pre>
     * schedule ---> start ---> close
     *              |_______________|
     * </pre>
     * Pass null unit or duration &lt;= 0 for a timeout of NONE.
     *
     * @see ScheduleActivityTaskDecisionAttributes#startToCloseTimeout
     */
    public ActivityAction withStartToCloseTimeout(TimeUnit unit, long duration) {
        this.startToCloseTimeout = calcTimeoutString(unit, duration);
        return this;
    }

    /**
     * Get the output of the child workflow.
     * Clients should ensure that the child workflow has finished successfully before calling this method.
     *
     * @throws UnsupportedOperationException if child output is not available
     * @see #getState()
     */
    public String getOutput() {
        if (!isSuccess()) {
            throw new UnsupportedOptionException(format("Result not available for activity %s with state %s", this, getState()));
        }
        return getCurrentHistoryEvent().getResult();
    }

    /**
     * @return decision of type {@link DecisionType#ScheduleActivityTask}
     */
    @Override
    public Decision createInitiateActivityDecision() {
        return new Decision()
            .withDecisionType(DecisionType.ScheduleActivityTask)
            .withScheduleActivityTaskDecisionAttributes(new ScheduleActivityTaskDecisionAttributes()
                .withActivityType(new ActivityType()
                    .withName(name)
                    .withVersion(version))
                .withActivityId(getActionId())
                .withTaskList(new TaskList()
                    .withName(getWorkflow().getTaskList()))
                .withInput(input)
                .withControl(control)
                .withHeartbeatTimeout(heartBeatTimeoutTimeout)
                .withScheduleToCloseTimeout(scheduleToCloseTimeout)
                .withScheduleToStartTimeout(scheduleToStartTimeout)
                .withStartToCloseTimeout(startToCloseTimeout));
    }

    @Override
    public String toString() {
        return format("%s %s %s", getClass().getSimpleName(), getActionId(), makeKey(name, version));
    }
}
