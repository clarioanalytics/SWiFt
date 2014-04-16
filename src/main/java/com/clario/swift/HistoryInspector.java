package com.clario.swift;

import com.amazonaws.services.simpleworkflow.model.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.amazonaws.services.simpleworkflow.model.EventType.*;
import static com.clario.swift.Checkpoint.CHECKPOINT_PREFIX;

/**
 * Helper class that contains convenience methods for working with a list of {@link HistoryEvent}.
 * Most of the heavy lifting comes from converting each {@link HistoryEvent} into a {@link TaskEvent}.
 * This class is meant to be used by a single {@link WorkflowPoller}.
 *
 * @author George Coller
 * @see TaskEvent
 * @see WorkflowPoller
 */
public class HistoryInspector {
    private String workflowId = "";
    private String runId = "";
    private List<TaskEvent> taskEvents = new ArrayList<>();
    private List<HistoryEvent> historyEvents = new ArrayList<>();
    private List<HistoryEvent> markerEvents = new ArrayList<>();
    private List<HistoryEvent> signalEvents = new ArrayList<>();
    private HistoryEvent workflowExecutionStarted;

    public void addHistoryEvents(List<HistoryEvent> historyEvents) {
        this.historyEvents.addAll(historyEvents);
        for (HistoryEvent event : historyEvents) {
            if (TaskEvent.isTaskEvent(event)) {
                taskEvents.add(new TaskEvent(event));
            }
            if (MarkerRecorded.name().equals(event.getEventType())) { markerEvents.add(event); }
            if (WorkflowExecutionSignaled.name().equals(event.getEventType())) { signalEvents.add(event); }
            if (WorkflowExecutionStarted.name().equals(event.getEventType())) { workflowExecutionStarted = event; }
        }
    }

    public void clear() {
        workflowId = "";
        runId = "";
        taskEvents.clear();
        historyEvents.clear();
        markerEvents.clear();
        signalEvents.clear();
        workflowExecutionStarted = null;
    }

    public boolean isEmpty() {
        return historyEvents.isEmpty();
    }

    /**
     * Return the list of {@link TaskEvent} for a given id.
     *
     * @param id unique id of the Activity, Timer, or ChildWorkflow
     *
     * @return the list, empty if none found
     */
    public List<TaskEvent> taskEvents(String id) {
        int index = -1;
        for (int i = 0; i < taskEvents.size(); i++) {
            if (taskEvents.get(i).isInitialTaskEvent() && taskEvents.get(i).getId().equals(id)) {
                index = i;
                break;
            }

        }

        List<TaskEvent> list = new ArrayList<>();
        if (index >= 0) {
            TaskEvent initial = taskEvents.get(index);
            for (TaskEvent it : taskEvents.subList(0, index + 1)) {
                if (it.equals(initial) || it.getInitialTaskEventId().equals(initial.getEventId())) {
                    list.add(it);
                }
            }
        }
        return list;
    }

    /**
     * @return Current checkpoint calculated as the max of available checkpoint signals or default zero.
     */
    public int getCurrentCheckpoint() {
        int i = 0;
        for (String signal : getSignals().keySet()) {
            if (signal.startsWith(CHECKPOINT_PREFIX)) {
                i = Math.max(i, Checkpoint.parseId(signal));
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
