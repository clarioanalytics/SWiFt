package com.clario.swift;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.amazonaws.services.simpleworkflow.model.EventType;

import java.util.*;

import static com.clario.swift.SwiftUtil.firstOrNull;
import static com.clario.swift.SwiftUtil.isNotEmpty;

/**
 * Base class for workflow task implementations.
 *
 * @author George Coller
 */
public abstract class Task implements Comparable<Task>, Vertex<Task> {
    /**
     * User-defined activity identifier, must be unique per instance.
     */
    private final String id;
    private final Set<Task> parents = new TreeSet<>();

    private int retryTimes;
    private long retryWaitInMillis;
    private HistoryInspector historyInspector;
    private MapSerializer ioSerializer = new MapSerializer();

    public Task(String id) {
        this.id = id;
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
            throw new IllegalStateException("Unknown TaskState " + this + ":\n" + SwiftUtil.join(taskEvents(), ", "));
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

    public String getWorkflowInput() {
        return historyInspector.getWorkflowInput();
    }

    EventType getCurrentEventType() {
        TaskEvent event = firstOrNull(taskEvents());
        return event == null ? null : event.getType();
    }

    /**
     * Return a map of available inputs.
     * Workflow input (if no parents and it is available) will be put in map with a empty string as the key.
     * Parent outputs will be put in map with the task id as key
     */
    public Map<String, String> getInputs() {
        final Map<String, String> input = new LinkedHashMap<>();
        if (parents.isEmpty() && isNotEmpty(getWorkflowInput())) {
            input.put("", getWorkflowInput());
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
        if (getSuccessEventType() == getCurrentEventType()) {
            return getIoSerializer().unmarshal(taskEvents().get(0).getResult());
        } else {
            throw new UnsupportedOperationException("Output not available: " + toString());
        }
    }

    /**
     * Add given tasks as parents
     *
     * @return this instance
     */
    public Task addParents(Task... parentTasks) {
        Collections.addAll(Task.this.parents, parentTasks);
        return this;
    }

    /**
     * Find a parent by <code>uniqueId</code>.
     *
     * @throws IllegalArgumentException if parent not found
     */
    public Task getParent(final String uniqueId) {
        for (Task parent : parents) {
            if (parent.getId().equals(uniqueId)) {
                return parent;
            }
        }
        throw new IllegalArgumentException("Parent not found: " + uniqueId);
    }

    public void addRetry(int times, long waitInMillis) {
        this.retryTimes = times;
        this.retryWaitInMillis = waitInMillis;
    }

    /**
     * Return list of {@link TaskEvent} belonging to this instance in {@link TaskEvent#getEventTimestamp()} newest-first order.
     */
    private List<TaskEvent> taskEvents() {
        return historyInspector.taskEvents(id);
    }

    public final String getId() {
        return id;
    }

    public final Set<Task> getParents() {
        return parents;
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

    public boolean equals(Object o) {
        return this == o || o instanceof Task && id.equals(((Task) o).id);
    }

    public int hashCode() {
        return id.hashCode();
    }

    public int compareTo(Task task) {
        return id.compareTo(task.id);
    }

    public String toString() {
        return getClass().getSimpleName() + ":" + id;
    }
}
