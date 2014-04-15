package com.clario.swift;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.amazonaws.services.simpleworkflow.model.EventType;

import java.util.*;

import static com.clario.swift.SwiftUtil.firstOrNull;
import static com.clario.swift.SwiftUtil.isNotEmpty;

/**
 * Base class for decision step implementations.
 *
 * @author George Coller
 */
public abstract class DecisionStep implements Comparable<DecisionStep>, Vertex {
    /**
     * User-defined activity identifier, must be unique per instance.
     */
    private final String stepId;
    private final Set<DecisionStep> parents = new TreeSet<>();

    /**
     * Decision group this step is part of.
     * Workflows that will generate many history events should be broken up into several decision groups.
     */
    private int decisionGroup = 0;
    private int retryTimes;
    private long retryWaitInMillis;
    private HistoryInspector historyInspector;
    private MapSerializer ioSerializer = new MapSerializer();

    public DecisionStep(String stepId) {
        this.stepId = stepId;
    }

    /**
     * @return EventType that indicates step completed successfully
     */
    public abstract EventType getSuccessEventType();

    /**
     * @return zero or more EventTypes that indicate step failed
     */
    public abstract List<EventType> getFailEventTypes();

    /**
     * Called on every poll if {@link #isCanDecide()} is true.
     */
    public abstract List<Decision> decide();

    /**
     * @return true if this step is finished, either in a valid or error state.
     */
    public boolean isStepFinished() {
        return getSuccessEventType() == getCurrentEventType() || isStepError();
    }

    /**
     * @return true if this step or one of it's parents is in an error state.
     */
    public boolean isStepError() {
        return getFailEventTypes().contains(getCurrentEventType()) || isParentStepErrors();
    }

    /**
     * @return true if one or more parents {@link #isStepError} is true.
     */
    public boolean isParentStepErrors() {
        for (DecisionStep parent : parents) {
            if (parent.isStepError()) { return true; }
        }
        return false;
    }

    /**
     * @return true if all parents {@link #isStepFinished} are true
     */
    public boolean isParentStepsFinished() {
        for (DecisionStep parent : parents) {
            if (!parent.isStepFinished()) { return false; }
        }
        return true;
    }

    /**
     * @return true if {@link #decide()} should be called on this poll.
     */
    public boolean isCanDecide() {
        return stepEvents().isEmpty() && isParentStepsFinished() && !isStepFinished();
    }

    // TODO: Implement get error count.
    public int getErrorCount() {
        throw new UnsupportedOperationException("method not implemented");
    }

    public String getWorkflowInput() {
        return historyInspector.getWorkflowInput();
    }

    EventType getCurrentEventType() {
        StepEvent event = firstOrNull(stepEvents());
        return event == null ? null : event.getType();
    }

    /**
     * Return a map of available inputs.
     * Workflow input (if no parents and it is available) will be put in map with a empty string as the key.
     * Parent outputs will be put in map with the step id as key
     */
    public Map<String, String> getInputs() {
        final Map<String, String> input = new LinkedHashMap<>();
        if (parents.isEmpty() && isNotEmpty(getWorkflowInput())) {
            input.put("", getWorkflowInput());
        } else {
            for (DecisionStep parent : parents) {
                input.putAll(parent.getOutput());
            }
        }
        return input;
    }

    /**
     * Get the output of this decision step if it produces one and has completed successfully.
     *
     * @return Map of outputs.
     * @throws UnsupportedOperationException if output is not available
     */
    public Map<String, String> getOutput() {
        if (getSuccessEventType() == getCurrentEventType()) {
            return getIoSerializer().unmarshal(stepEvents().get(0).getResult());
        } else {
            throw new UnsupportedOperationException("Output not available: " + toString());
        }
    }

    /**
     * Add given steps as parents
     *
     * @return this instance
     */
    public DecisionStep addParents(DecisionStep... parentDecisionSteps) {
        for (DecisionStep step : parentDecisionSteps) {
            DecisionStep.this.parents.add(step);
//            step.children.add(DecisionStep.this);
        }
        return this;
    }

    /**
     * Find a parent by <code>uniqueId</code>.
     *
     * @throws IllegalArgumentException if parent not found
     */
    public DecisionStep getParent(final String uniqueId) {
        for (DecisionStep parent : parents) {
            if (parent.getStepId().equals(uniqueId)) {
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
     * Return list of {@link com.clario.swift.StepEvent} belonging to this instance in {@link com.clario.swift.StepEvent#getEventTimestamp()} order.
     */
    public List<StepEvent> stepEvents() {
        return historyInspector.stepEvents(stepId);
    }

    public final String getStepId() {
        return stepId;
    }

    public final Set<DecisionStep> getParents() {
        return parents;
    }

    public int getDecisionGroup() {
        return decisionGroup;
    }

    public void setDecisionGroup(int decisionGroup) {
        this.decisionGroup = decisionGroup;
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
        return this == o || o instanceof DecisionStep && stepId.equals(((DecisionStep) o).stepId);
    }

    @Override
    public int hashCode() {
        return stepId.hashCode();
    }

    @Override
    public int compareTo(DecisionStep decisionStep) {
        return stepId.compareTo(decisionStep.stepId);
    }

    public String toString() {
        return getClass().getSimpleName() + ":" + stepId;
    }

}
