package com.clario.swift

import com.amazonaws.services.simpleworkflow.model.EventType
import com.amazonaws.services.simpleworkflow.model.HistoryEvent

import static DecisionGroupDecisionStep.DECISION_GROUP_PREFIX
import static com.amazonaws.services.simpleworkflow.model.EventType.*

/**
 * Helper class that contains convenience methods for working with a list of {@link HistoryEvent}.
 * Most of the heavy lifting comes from converting each {@link HistoryEvent} into a {@link StepEvent}.
 * This class is meant to be used by a single {@link WorkflowPoller}.
 *
 * @author George Coller
 * @see StepEvent
 * @see WorkflowPoller
 */
public class HistoryInspector {

    String workflowId = ""
    String runId = ""
    private List<StepEvent> stepEvents = []
    private List<HistoryEvent> historyEvents = []
    private List<HistoryEvent> markerEvents = []
    private List<HistoryEvent> signalEvents = []
    private HistoryEvent workflowExecutionStarted

    void addHistoryEvents(List<HistoryEvent> historyEvents) {
        this.historyEvents.addAll(historyEvents)
        stepEvents.addAll(historyEvents.findAll { StepEvent.isStepEvent(it) }.collect { new StepEvent(it) })

        markerEvents.addAll(historyEvents.findAll { MarkerRecorded.name() == it.eventType })
        signalEvents.addAll(historyEvents.findAll { WorkflowExecutionSignaled.name() == it.eventType })
        workflowExecutionStarted = historyEvents.find { WorkflowExecutionStarted.name() == it.eventType }
    }

    void clear() {
        workflowId = ""
        runId = ""
        stepEvents.clear()
        historyEvents.clear()
        markerEvents.clear()
        signalEvents.clear()
        workflowExecutionStarted = null
    }

    boolean isEmpty() {
        historyEvents.isEmpty()
    }

    /**
     * Return the list of {@link StepEvent} for a given stepId.
     * @param stepId unique id of the Activity, Timer, or ChildWorkflow
     * @return the list, empty if none found
     */
    List<StepEvent> stepEvents(String stepId) {
        int index = stepEvents.findIndexOf {
            it.initialStepEvent && stepId == it.stepId
        }
        if (index >= 0) {
            StepEvent initial = stepEvents[index]
            def list = stepEvents.subList(0, index + 1).findAll {
                it == initial || it.initialStepEventId == initial.eventId
            }
            list
        } else {
            []
        }
    }

    /**
     * @return Current decision group calculated as the max of available decision group decisions or default zero.
     */
    int getDecisionGroup() {
        int i = signals.keySet().findAll {
            it.startsWith(DECISION_GROUP_PREFIX)
        }.inject(0) { int num, String signalName ->
            def id = DecisionGroupDecisionStep.parseStepId(signalName)
            Math.max(num, id)
        } as Integer
        i
    }

    /**
     * @return events with type {@link EventType#MarkerRecorded} converted to a map of marker name, details entries.
     */
    Map<String, String> getMarkers() {
        markerEvents.collectEntries {
            def attributes = it.markerRecordedEventAttributes
            [attributes.markerName, attributes.details]
        }
    }

    /**
     * @return events with type {@link EventType#WorkflowExecutionSignaled} converted to a map of signal name, input entries.
     */
    Map<String, String> getSignals() {
        signalEvents.collectEntries {
            def attributes = it.workflowExecutionSignaledEventAttributes
            [attributes.signalName, attributes.input]
        }
    }

    String getWorkflowInput() {
        workflowExecutionStarted?.workflowExecutionStartedEventAttributes?.input
    }

}