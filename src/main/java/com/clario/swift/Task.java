package com.clario.swift;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.amazonaws.services.simpleworkflow.model.EventType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.clario.swift.SwiftUtil.isNotEmpty;

/**
 * Base class for workflow task implementations.
 *
 * @author George Coller
 */
public abstract class Task extends Vertex<Task> {

    private HistoryInspector historyInspector;
    private MapSerializer ioSerializer = new MapSerializer();
    private int retryTimes;
    private long retryWaitInMillis;

    public Task(String id) {
        super(id);
    }

    /**
     * @return EventType that indicates task has been scheduled
     */
    public abstract EventType getScheduledEventType();

    /**
     * @return EventType that indicates task completed successfully
     */
    public abstract EventType getSuccessEventType();

    /**
     * @return zero or more EventTypes that indicate task failed
     */
    public abstract List<EventType> getFailEventTypes();

    /**
     * Called on poll if task state is {@link TaskState#ready_to_decide}.
     */
    public abstract List<Decision> decide();

    public TaskState getState() {
        EventType currentEventType = getCurrentEventType();
        if (currentEventType == null) {
            for (Task parent : parents) {
                TaskState state = parent.getState();
                if (state.isFinished()) {
                    if (state.isErrorOrCancel()) {
                        return TaskState.finish_cancel;
                    }
                } else {
                    return TaskState.wait_for_parents;
                }
            }
            return TaskState.ready_to_decide;
        } else if (currentEventType == getScheduledEventType()) {
            return TaskState.scheduled;
        } else if (currentEventType == getSuccessEventType()) {
            return TaskState.finish_ok;
        } else if (getFailEventTypes().contains(currentEventType)) {
            return TaskState.finish_error;
        } else {
            throw new IllegalStateException("Unknown TaskState " + this + ":\n" + SwiftUtil.join(getTaskEvents(), ", "));
        }
    }

    public boolean isMoreHistoryRequired() {
        if (getCurrentEventType() == null) {
            for (Task parent : parents) {
                if (parent.isMoreHistoryRequired()) {
                    return true;
                }
            }
            return false;
        } else {
            return false;
        }
    }

    TaskEvent getCurrentTaskEvent() {
        List<TaskEvent> events = getTaskEvents();
        return events.isEmpty() ? null : events.get(0);
    }

    private List<TaskEvent> getTaskEvents() {return historyInspector.taskEvents(id);}

    EventType getCurrentEventType() {
        TaskEvent event = getCurrentTaskEvent();
        return event == null ? null : event.getType();
    }

    /**
     * Return a map of available inputs.
     * Workflow input (if no parents and it is available) will be put in map with a empty string as the key.
     * Parent outputs will be put in map with the task id as key
     */
    public Map<String, String> getInputs() {
        final Map<String, String> input = new LinkedHashMap<>();
        String workflowInput = historyInspector.getWorkflowInput();
        if (parents.isEmpty() && isNotEmpty(workflowInput)) {
            input.put("", workflowInput);
        } else {
            for (Task parent : parents) {
                input.putAll(parent.getOutput());
            }
        }
        return input;
    }

    /**
     * Get the output of this decision task if it produces one and has completed successfully.
     *
     * @return Map of outputs.
     * @throws UnsupportedOperationException if output is not available
     */
    public Map<String, String> getOutput() {
        TaskEvent current = getCurrentTaskEvent();
        if (getSuccessEventType() == current.getType()) {
            return getIoSerializer().unmarshal(current.getResult());
        } else {
            throw new UnsupportedOperationException("Output not available: " + toString());
        }
    }


    public void addRetry(int times, long waitInMillis) {
        this.retryTimes = times;
        this.retryWaitInMillis = waitInMillis;
    }

    public void setHistoryInspector(HistoryInspector historyInspector) {
        this.historyInspector = historyInspector;
    }

    public MapSerializer getIoSerializer() {
        return ioSerializer;
    }

    public void setIoSerializer(MapSerializer ioSerializer) {
        this.ioSerializer = ioSerializer;
    }
}
