package com.clario.swift.action;

import com.amazonaws.services.simpleworkflow.model.*;

import java.util.concurrent.TimeUnit;

import static com.clario.swift.SwiftUtil.*;
import static java.lang.String.format;

/**
 * Maps to a registered SWF Activity.
 *
 * @author George Coller
 */
public class ActivityAction extends Action<ActivityAction> {
    private String name;
    private String version;
    private String taskList;
    private String input;
    private String control;
    private String heartBeatTimeoutTimeout = SWF_TIMEOUT_NONE;
    private String scheduleToCloseTimeout = SWF_TIMEOUT_NONE;
    private String scheduleToStartTimeout = SWF_TIMEOUT_NONE;
    private String startToCloseTimeout = SWF_TIMEOUT_NONE;

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
        this.name = assertSwfValue(assertMaxLength(name, MAX_NAME_LENGTH));
        this.version = assertSwfValue(assertMaxLength(version, MAX_VERSION_LENGTH));
        return this;
    }

    public String getName() { return name; }

    public String getVersion() { return version; }

    /**
     * @see ScheduleActivityTaskDecisionAttributes#input
     */
    public ActivityAction withInput(String input) {
        this.input = assertMaxLength(input, MAX_INPUT_LENGTH);
        return this;
    }

    public String getInput() { return input; }

    /**
     * Set the task list for this activity.
     * If not set the activity will use its related workflow task list.
     * This allows for sending activity tasks to different lists.
     *
     * @param input task list
     */
    public ActivityAction withTaskList(String input) {
        this.taskList = input;
        return this;
    }

    public String getTaskList() { return taskList; }

    /**
     * @see ScheduleActivityTaskDecisionAttributes#control
     */
    public ActivityAction withControl(String control) {
        this.control = assertMaxLength(control, MAX_CONTROL_LENGTH);
        return this;
    }

    public String getControl() { return control; }


    /**
     * Set all timeout values to null instead of the default "NONE".
     * <p/>
     * By default "NONE" is set on all activity action timeouts to avoid SWF API
     * errors when attempting to execute an activity that was registered without
     * default values.
     * <p/>
     * This method can be used in cases where you know the activity was registered with
     * default values and you want to use those default values. For instance a subclass
     * can call this method in its constructor.
     *
     * @see #withHeartBeatTimeoutTimeout
     * @see #withScheduleToCloseTimeout
     * @see #withScheduleToStartTimeout
     * @see #withStartToCloseTimeout
     */
    public ActivityAction withUnsetDefaultTimeouts() {
        heartBeatTimeoutTimeout = null;
        scheduleToCloseTimeout = null;
        scheduleToStartTimeout = null;
        startToCloseTimeout = null;
        return this;
    }

    /**
     * Override activity's default heartbeat timeout.
     * Pass null unit or duration &lt;= 0 for a timeout of NONE.
     *
     * @see #withUnsetDefaultTimeouts()
     * @see ScheduleActivityTaskDecisionAttributes#heartbeatTimeout
     */
    public ActivityAction withHeartBeatTimeoutTimeout(TimeUnit unit, long duration) {
        this.heartBeatTimeoutTimeout = calcTimeoutOrNone(unit, duration);
        return this;
    }

    public String getHeartBeatTimeoutTimeout() { return heartBeatTimeoutTimeout; }

    /**
     * Override activity's default schedule to close timeout.
     * <pre>
     * schedule ---> start ---> close
     * |_____________________________|
     * </pre>
     * Pass null unit or duration &lt;= 0 for a timeout of NONE.
     *
     * @see #withUnsetDefaultTimeouts()
     * @see ScheduleActivityTaskDecisionAttributes#scheduleToCloseTimeout
     */
    public ActivityAction withScheduleToCloseTimeout(TimeUnit unit, long duration) {
        this.scheduleToCloseTimeout = calcTimeoutOrNone(unit, duration);
        return this;
    }

    public String getScheduleToCloseTimeout() { return scheduleToCloseTimeout; }

    /**
     * Override activity's default schedule to close timeout.
     * <pre>
     * schedule ---> start ---> close
     * |_________________|
     * </pre>
     * Pass null unit or duration &lt;= 0 for a timeout of NONE.
     *
     * @see #withUnsetDefaultTimeouts()
     * @see ScheduleActivityTaskDecisionAttributes#scheduleToStartTimeout
     */
    public ActivityAction withScheduleToStartTimeout(TimeUnit unit, long duration) {
        this.scheduleToStartTimeout = calcTimeoutOrNone(unit, duration);
        return this;
    }

    public String getScheduleToStartTimeout() { return scheduleToStartTimeout; }


    /**
     * Override activity's default start to close timeout.
     * <pre>
     * schedule ---> start ---> close
     *              |_______________|
     * </pre>
     * Pass null unit or duration &lt;= 0 for a timeout of NONE.
     *
     * @see #withUnsetDefaultTimeouts()
     * @see ScheduleActivityTaskDecisionAttributes#startToCloseTimeout
     */
    public ActivityAction withStartToCloseTimeout(TimeUnit unit, long duration) {
        this.startToCloseTimeout = calcTimeoutOrNone(unit, duration);
        return this;
    }

    public String getStartToCloseTimeout() { return startToCloseTimeout; }

    @Override
    protected ActivityAction thisObject() { return this; }

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
                    .withName(taskList == null ? getWorkflow().getTaskList() : taskList))
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
