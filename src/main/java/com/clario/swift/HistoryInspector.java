package com.clario.swift;

import com.amazonaws.services.simpleworkflow.model.EventType;
import com.amazonaws.services.simpleworkflow.model.HistoryEvent;
import com.amazonaws.services.simpleworkflow.model.MarkerRecordedEventAttributes;
import com.amazonaws.services.simpleworkflow.model.WorkflowExecutionSignaledEventAttributes;

import java.util.*;

import static com.amazonaws.services.simpleworkflow.model.EventType.*;

/**
 * Helper class that contains convenience methods for working with a list of {@link HistoryEvent}.
 * Most of the heavy lifting comes from converting each {@link HistoryEvent} into a {@link TaskEvent}.
 * This class is not thread-safe and is meant to be used by a single {@link Workflow} instance.
 *
 * @author George Coller
 * @see TaskEvent
 * @see Workflow
 */
public class HistoryInspector {
    private LinkedList<TaskEvent> taskEvents = new LinkedList<>();
    private List<HistoryEvent> historyEvents = new ArrayList<>();
    private List<HistoryEvent> markerEvents = new ArrayList<>();
    private List<HistoryEvent> signalEvents = new ArrayList<>();
    private HistoryEvent workflowExecutionStarted;

    public void addHistoryEvents(List<HistoryEvent> historyEvents) {
        // Note: historyEvents are sorted newest to oldest
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

    /**
     * Reset instance to prepare for new set of history.
     */
    public void reset() {
        taskEvents.clear();
        historyEvents.clear();
        markerEvents.clear();
        signalEvents.clear();
        workflowExecutionStarted = null;
    }

    /**
     * Return the list of {@link TaskEvent} for a given task id.
     *
     * @param taskId unique id of the Activity, Timer, or ChildWorkflow
     *
     * @return the list, empty if none found
     */
    public List<TaskEvent> taskEvents(String taskId) {
        List<TaskEvent> list = new ArrayList<>();

        // iterate backwards through list (need to find initial event first)
        Iterator<TaskEvent> iter = taskEvents.descendingIterator();
        long initialId = -1;
        while (iter.hasNext()) {
            TaskEvent event = iter.next();
            if (event.isInitialTaskEvent() && event.getTaskId().equals(taskId)) {
                initialId = event.getEventId();
                list.add(event);
            } else if (initialId == event.getInitialTaskEventId()) {
                list.add(event);
            }
        }
        Collections.reverse(list);
        return list;
    }

    public static boolean contains(List<TaskEvent> events, EventType eventType) {
        for (TaskEvent taskEvent : events) {
            if (taskEvent.getType() == eventType) {
                return true;
            }
        }
        return false;
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

    /**
     * If available return the input string given to this workflow when it was initiated on SWF.
     *
     * @return the input or null if not available
     */
    public String getWorkflowInput() {
        if (workflowExecutionStarted == null) {
            return null;
        } else {
            return workflowExecutionStarted.getWorkflowExecutionStartedEventAttributes().getInput();
        }
    }
}
