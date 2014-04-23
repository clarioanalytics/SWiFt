package com.clario.swift;

import com.amazonaws.services.simpleworkflow.model.EventType;
import com.amazonaws.services.simpleworkflow.model.HistoryEvent;
import com.amazonaws.services.simpleworkflow.model.MarkerRecordedEventAttributes;
import com.amazonaws.services.simpleworkflow.model.WorkflowExecutionSignaledEventAttributes;

import java.util.*;

import static com.amazonaws.services.simpleworkflow.model.EventType.*;

/**
 * Helper class that contains convenience methods for working with a list of {@link SwfHistoryEvent}.
 * Most of the heavy lifting comes from converting each SWF {@link HistoryEvent} into a {@link SwfHistoryEvent} to
 * unify the event API across * various SWF tasks like Activities, Timers, Markers, Signals, ChildWorkflows.
 * <p/>
 * This class is not thread-safe and is meant to be used by a single {@link Workflow} instance.
 *
 * @author George Coller
 * @see SwfHistoryEvent
 * @see Workflow
 */
public class SwfHistory {
    private LinkedList<SwfHistoryEvent> swfHistoryEvents = new LinkedList<>();
    private List<HistoryEvent> historyEvents = new ArrayList<>();
    private List<HistoryEvent> markerEvents = new ArrayList<>();
    private List<HistoryEvent> signalEvents = new ArrayList<>();
    private List<HistoryEvent> workflowStateErrors = new ArrayList<>();
    private HistoryEvent workflowExecutionStarted;

    public void addHistoryEvents(List<HistoryEvent> historyEvents) {
        // Note: historyEvents are sorted newest to oldest
        this.historyEvents.addAll(historyEvents);
        for (HistoryEvent event : historyEvents) {
            if (SwfHistoryEvent.isActionHistoryEvent(event)) {
                swfHistoryEvents.add(new SwfHistoryEvent(event));
            }
            EventType eventType = EventType.valueOf(event.getEventType());
            if (MarkerRecorded == eventType) { markerEvents.add(event); }
            if (WorkflowExecutionSignaled == eventType) { signalEvents.add(event); }
            if (WorkflowExecutionStarted == eventType) { workflowExecutionStarted = event; }
            if (ScheduleActivityTaskFailed == eventType
                || WorkflowExecutionCancelRequested == eventType
                || StartChildWorkflowExecutionFailed == eventType
                || SignalExternalWorkflowExecutionFailed == eventType
                ) {
                workflowStateErrors.add(event);
            }
        }
    }

    /**
     * Reset instance to prepare for new set of history.
     */
    public void reset() {
        swfHistoryEvents.clear();
        historyEvents.clear();
        markerEvents.clear();
        signalEvents.clear();
        workflowStateErrors.clear();
        workflowExecutionStarted = null;
    }

    /**
     * Return the list of {@link SwfHistoryEvent} for a given action id.
     *
     * @param activityId unique id of the Activity, Timer, or ChildWorkflow
     *
     * @return the list, empty if none found
     */
    public List<SwfHistoryEvent> actionEvents(String activityId) {
        List<SwfHistoryEvent> list = new ArrayList<>();

        // iterate backwards through list (need to find initial event first)
        Iterator<SwfHistoryEvent> iter = swfHistoryEvents.descendingIterator();
        long initialId = -1;
        while (iter.hasNext()) {
            SwfHistoryEvent event = iter.next();
            if (event.isInitialEvent() && event.getActionId().equals(activityId)) {
                initialId = event.getEventId();
                list.add(event);
            } else if (initialId == event.getInitialEventId()) {
                list.add(event);
            }
        }
        Collections.reverse(list);
        return list;
    }

    public static boolean contains(List<SwfHistoryEvent> events, EventType eventType) {
        for (SwfHistoryEvent swfHistoryEvent : events) {
            if (swfHistoryEvent.getType() == eventType) {
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

    public List<HistoryEvent> getWorkflowStateErrors() {
        return workflowStateErrors;
    }
}
