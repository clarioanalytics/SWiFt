package com.clario.swift;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.amazonaws.services.simpleworkflow.model.EventType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.amazonaws.services.simpleworkflow.model.EventType.*;
import static com.clario.swift.SwiftUtil.createScheduleActivityTaskDecision;
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
    public Map<EventType, TaskState> getEventTypeTaskStateMap() {
        Map<EventType, TaskState> map = new LinkedHashMap<>();
        map.put(ActivityTaskScheduled, TaskState.scheduled);
        map.put(ActivityTaskStarted, TaskState.scheduled);
        map.put(ActivityTaskCompleted, TaskState.finish_ok);
        map.put(ActivityTaskCanceled, TaskState.finish_cancel);
        map.put(ActivityTaskTimedOut, TaskState.finish_cancel);
        map.put(ActivityTaskFailed, TaskState.finish_error);
        return map;
    }

    /**
     * Default implementation is to schedule this instances activity with all available inputs
     * serialized using this instance's {@link com.clario.swift.MapSerializer}.
     */
    @Override
    public List<Decision> decide() {
        String input = getIoSerializer().marshal(getInputs());
        return asList(scheduleActivityDecision(input));
    }

    protected Decision scheduleActivityDecision(String input) {
        return createScheduleActivityTaskDecision(
            getId(), name, version, taskList, input, control, heartBeatTimeoutTimeout, scheduleToCloseTimeout, scheduleToStartTimeout, startToCloseTimeout
        );
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

    public String getName() { return name; }

    public String getVersion() { return version; }

    public String getControl() { return control; }

    public void setControl(String control) { this.control = control; }

    public String getTaskList() { return taskList; }

    public void setTaskList(String taskList) { this.taskList = taskList; }
}
