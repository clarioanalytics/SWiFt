package com.clario.swift;

import com.amazonaws.services.simpleworkflow.model.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.amazonaws.services.simpleworkflow.model.EventType.*;
import static com.clario.swift.DecisionGroupDecisionStep.DECISION_GROUP_PREFIX;

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
    private String workflowId = "";
    private String runId = "";
    private List<StepEvent> stepEvents = new ArrayList<>();
    private List<HistoryEvent> historyEvents = new ArrayList<>();
    private List<HistoryEvent> markerEvents = new ArrayList<>();
    private List<HistoryEvent> signalEvents = new ArrayList<>();
    private HistoryEvent workflowExecutionStarted;

    public void addHistoryEvents(List<HistoryEvent> historyEvents) {
        this.historyEvents.addAll(historyEvents);
        for (HistoryEvent event : historyEvents) {
            if (StepEvent.isStepEvent(event)) {
                stepEvents.add(new StepEvent(event));
            }
            if (MarkerRecorded.name().equals(event.getEventType())) { markerEvents.add(event); }
            if (WorkflowExecutionSignaled.name().equals(event.getEventType())) { signalEvents.add(event); }
            if (WorkflowExecutionStarted.name().equals(event.getEventType())) { workflowExecutionStarted = event; }
        }
    }

    public void clear() {
        workflowId = "";
        runId = "";
        stepEvents.clear();
        historyEvents.clear();
        markerEvents.clear();
        signalEvents.clear();
        workflowExecutionStarted = null;
    }

    public boolean isEmpty() {
        return historyEvents.isEmpty();
    }

    /**
     * Return the list of {@link StepEvent} for a given stepId.
     *
     * @param stepId unique id of the Activity, Timer, or ChildWorkflow
     *
     * @return the list, empty if none found
     */
    public List<StepEvent> stepEvents(String stepId) {
        int index = -1;
        for (int i = 0; i < stepEvents.size(); i++) {
            if (stepEvents.get(i).isInitialStepEvent() && stepEvents.get(i).getStepId().equals(stepId)) {
                index = i;
                break;
            }

        }

        List<StepEvent> list = new ArrayList<>();
        if (index >= 0) {
            StepEvent initial = stepEvents.get(index);
            for (StepEvent it : stepEvents.subList(0, index + 1)) {
                if (it.equals(initial) || it.getInitialStepEventId().equals(initial.getEventId())) {
                    list.add(it);
                }
            }
        }
        return list;
    }

    /**
     * @return Current decision group calculated as the max of available decision group decisions or default zero.
     */
    public int getDecisionGroup() {
        int i = 0;
        for (String signal : getSignals().keySet()) {
            if (signal.startsWith(DECISION_GROUP_PREFIX)) {
                i = Math.max(i, DecisionGroupDecisionStep.parseStepId(signal));
            }
        }
        return i;
    }

    /**
     * @return events with type {@link EventType#MarkerRecorded} converted to a map of marker name, details entries.
     */
    public Map<String, String> getMarkers() {
        Map<String, String> markers = new LinkedHashMap<>();
        for (HistoryEvent it : markerEvents) {
            MarkerRecordedEventAttributes attributes = it.getMarkerRecordedEventAttributes();
            markers.put(attributes.getMarkerName(), attributes.getDetails());
        }
        return markers;
    }

    /**
     * @return events with type {@link EventType#WorkflowExecutionSignaled} converted to a map of signal name, input entries.
     */
    public Map<String, String> getSignals() {
        Map<String, String> signals = new LinkedHashMap<>();
        for (HistoryEvent it : signalEvents) {
            WorkflowExecutionSignaledEventAttributes attributes = it.getWorkflowExecutionSignaledEventAttributes();
            signals.put(attributes.getSignalName(), attributes.getInput());
        }
        return signals;
    }

    public String getWorkflowInput() {
        WorkflowExecutionStartedEventAttributes attrs = workflowExecutionStarted == null ? null : workflowExecutionStarted.getWorkflowExecutionStartedEventAttributes();
        return attrs == null ? null : attrs.getInput();
    }

    public String getWorkflowId() {
        return workflowId;
    }

    public void setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }
}
