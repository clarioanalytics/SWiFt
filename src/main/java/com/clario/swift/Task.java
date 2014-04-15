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
public abstract class Task implements Comparable<Task>, Vertex {
    /**
     * User-defined activity identifier, must be unique per instance.
     */
    private final String id;
    private final Set<Task> parents = new TreeSet<>();

    /**
     * Breakpoint this task is part of.
     * Workflows that will generate many history events should be broken up into several breakpoints.
     */
    private int breakpoint = 0;
    private int retryTimes;
    private long retryWaitInMillis;
    private HistoryInspector historyInspector;
    private MapSerializer ioSerializer = new MapSerializer();

    public Task(String id) {
        this.id = id;
    }

    /**
     * @return EventType that indicates task completed successfully
     */
    public abstract EventType getSuccessEventType();

    /**
     * @return zero or more EventTypes that indicate task failed
     */
    public abstract List<EventType> getFailEventTypes();

    /**
     * Called on every poll if {@link #isCanDecide()} is true.
     */
    public abstract List<Decision> decide();

    /**
     * @return true if this task is finished, either in a valid or error state.
     */
    public boolean isTaskFinished() {
        return getSuccessEventType() == getCurrentEventType() || isTaskError();
    }

    /**
     * @return true if this task or one of it's parents is in an error state.
     */
    public boolean isTaskError() {
        return getFailEventTypes().contains(getCurrentEventType()) || isParentTaskErrors();
    }

    /**
     * @return true if one or more parents {@link #isTaskError} is true.
     */
    public boolean isParentTaskErrors() {
        for (Task parent : parents) {
            if (parent.isTaskError()) { return true; }
        }
        return false;
    }

    /**
     * @return true if all parents {@link #isTaskFinished} are true
     */
    public boolean isParentTasksFinished() {
        for (Task parent : parents) {
            if (!parent.isTaskFinished()) { return false; }
        }
        return true;
    }

    /**
     * @return true if {@link #decide()} should be called on this poll.
     */
    public boolean isCanDecide() {
        return taskEvents().isEmpty() && isParentTasksFinished() && !isTaskFinished();
    }

    // TODO: Implement get error count.
    public int getErrorCount() {
        throw new UnsupportedOperationException("method not implemented");
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
     * Return list of {@link TaskEvent} belonging to this instance in {@link TaskEvent#getEventTimestamp()} order.
     */
    public List<TaskEvent> taskEvents() {
        return historyInspector.taskEvents(id);
    }

    public final String getId() {
        return id;
    }

    public final Set<Task> getParents() {
        return parents;
    }

    public int getBreakpoint() {
        return breakpoint;
    }

    public void setBreakpoint(int breakpoint) {
        this.breakpoint = breakpoint;
    }

    public HistoryInspector getHistoryInspector() {
        return historyInspector;
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

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof Task && id.equals(((Task) o).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public int compareTo(Task task) {
        return id.compareTo(task.id);
    }

    public String toString() {
        return getClass().getSimpleName() + ":" + id;
    }

}
