package com.clario.swift

import com.amazonaws.services.simpleworkflow.model.Decision
import com.amazonaws.services.simpleworkflow.model.EventType

/**
 * Base class for decision step implementations.
 * @author George Coller
 */
public abstract class DecisionStep {

    /**
     * User-defined activity identifier, must be unique per instance.
     */
    final String stepId

    final List<DecisionStep> parents = []
    final List<DecisionStep> children = []

    /**
     * Decision group this step is part of.
     * Workflows that will generate many history events should be broken up into several decision groups.
     */
    int decisionGroup = 0
    protected int retryTimes
    protected int retryWaitInMillis

    DecisionStep(String stepId) {
        this.stepId = stepId
    }

    HistoryInspector historyInspector

    MapSerializer ioSerializer = new MapSerializer()

    /**
     * Return a list of {@link EventType} that signal that this step is finished.
     * <p/>
     * The list is expected to have:
     * <ul>
     *     <li>exactly one <code>EventType</code> that indicates the step finished successfully and must be the first item in the list</li>
     *     <li>zero or more <code>EventType</code> that indicates the step finished in an error state</li>
     * </ul>
     */
    abstract List<EventType> getFinalEventTypes()

    /**
     * Called on every poll if {@link #isCanDecide()} is true.
     */
    abstract List<Decision> decide();

    /**
     * @return true if this step is finished, either in a valid or error state.
     */
    boolean isStepFinished() {
        return finalEventTypes.head() == currentStepEvent?.type || stepError
    }

    /**
     * @return true if this step finished in an error state.
     */
    boolean isStepError() {
        finalEventTypes.tail().contains(currentStepEvent?.type) || parentStepErrors
    }

    /**
     * @return true if one or more parents {@link #isStepError} is true.
     */
    boolean isParentStepErrors() {
        return parents.find { it.stepError }
    }

    /**
     * @return true if all parents {@link #isStepFinished} are true
     */
    boolean isParentStepsFinished() {
        return parents.every { it.stepFinished }
    }

    /**
     * @return true if {@link #decide()} should be called on this poll.
     */
    boolean isCanDecide() {
        return stepEvents().isEmpty() && parentStepsFinished && !stepFinished
    }

    int errorCount() {

    }

    String getWorkflowInput() {
        return historyInspector.workflowInput
    }

    /**
     * @return most recent step event or null if none exists.
     */
    StepEvent getCurrentStepEvent() {
        def events = stepEvents()
        if (events) {
            return events.head()
        } else {
            return null
        }
    }

    /**
     * Return a map of available inputs.
     * Workflow input (if no parents and it is available) will be put in map with a empty string as the key.
     * Parent outputs will be put in map with the step id as key
     */
    Map<String, String> getInputs() {
        Map input = [:]
        if (parents.isEmpty() && getWorkflowInput()) {
            input[""] = getWorkflowInput()
        }
        parents.each {
            input.putAll(it.output)
        }
        input
    }

    /**
     * @return Map of outputs.
     */
    abstract Map<String, String> getOutput()

    /**
     * Add given steps as parents
     * @param parentDecisionSteps
     * @return this instance
     */
    DecisionStep addParents(DecisionStep... parentDecisionSteps) {
        parentDecisionSteps.each {
            this.parents.add(it)
            it.children.add(this)
        }
        this
    }

    /**
     * Find a parent by <code>uniqueId</code>.
     * @throws IllegalArgumentException if parent not found
     */
    DecisionStep getParent(String uniqueId) {
        def parent = parents.find { it.stepId == uniqueId }
        if (!parent) {
            throw new IllegalArgumentException("Parent not found: $uniqueId")
        }
        parent
    }

    void addRetry(int times, long waitInMillis) {
        this.retryTimes = times
        this.retryWaitInMillis = waitInMillis
    }

    /**
     * Return list of {@link StepEvent} belonging to this instance in {@link StepEvent#getEventTimestamp()} order.
     */
    List<StepEvent> stepEvents() {
        historyInspector.stepEvents(stepId)
    }

    boolean equals(o) {
        if (this.is(o)) {
            return true
        }
        if (getClass() != o.class) {
            return false
        }

        DecisionStep that = (DecisionStep) o

        if (stepId != that.stepId) {
            return false
        }
        return true
    }

    int hashCode() {
        return stepId.hashCode()
    }

    String toString() {
        return "${getClass().simpleName}:$stepId"
    }

}