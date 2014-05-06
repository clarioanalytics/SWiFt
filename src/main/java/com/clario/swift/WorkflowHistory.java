package com.clario.swift;

import com.amazonaws.services.simpleworkflow.model.EventType;
import com.amazonaws.services.simpleworkflow.model.HistoryEvent;
import com.amazonaws.services.simpleworkflow.model.MarkerRecordedEventAttributes;
import com.amazonaws.services.simpleworkflow.model.WorkflowExecutionSignaledEventAttributes;

import java.util.*;

import static com.amazonaws.services.simpleworkflow.model.EventType.TimerStarted;
import static com.clario.swift.action.RetryPolicy.RETRY_CONTROL_VALUE;

/**
 * Container class of {@link HistoryEvent}s for the current decision task.
 * <p/>
 * Most of the heavy lifting comes from converting SWF {@link HistoryEvent} into a {@link ActionHistoryEvent} to
 * unify working with various SWF tasks like activities,timers, signals, starting child workflows.
 * <p/>
 * This class will also parse out any received signals, markers, and workflow-level errors.
 * <p/>
 * This class is not thread-safe and is meant to be used by a single {@link Workflow} instance.
 *
 * @author George Coller
 * @see ActionHistoryEvent
 * @see Workflow
 */
public class WorkflowHistory {
    private final LinkedList<ActionHistoryEvent> actionEvents = new LinkedList<>();
    private final List<HistoryEvent> markerEvents = new ArrayList<>();
    private final List<HistoryEvent> signalEvents = new ArrayList<>();
    private final List<HistoryEvent> errorEvents = new ArrayList<>();
    private HistoryEvent workflowExecutionStarted;

    public void addHistoryEvents(List<HistoryEvent> historyEvents) {
        // Note: historyEvents are sorted newest to oldest
        for (HistoryEvent event : historyEvents) {
            if (ActionHistoryEvent.isActionHistoryEvent(event)) {
                actionEvents.add(new ActionHistoryEvent(event));
            }

            switch (EventType.valueOf(event.getEventType())) {
                case MarkerRecorded:
                    markerEvents.add(event);
                    break;
                case WorkflowExecutionSignaled:
                    signalEvents.add(event);
                    break;
                case WorkflowExecutionStarted:
                    workflowExecutionStarted = event;
                    break;

                // Events that can't be recovered from, config or state problems, etc.
                case WorkflowExecutionCancelRequested:
                case ScheduleActivityTaskFailed:
                case StartChildWorkflowExecutionFailed:
                case SignalExternalWorkflowExecutionFailed:
                    errorEvents.add(event);
                    break;
            }
        }
    }

    /**
     * Reset instance to prepare for new set of history.
     */
    public void reset() {
        actionEvents.clear();
        markerEvents.clear();
        signalEvents.clear();
        errorEvents.clear();
        workflowExecutionStarted = null;
    }

    /**
     * Return the current list of {@link ActionHistoryEvent} polled for a action.
     * The list is sorted by event timestamp in descending order (most recent first).
     *
     * @param actionId unique id of the action.
     *
     * @return the list, empty if none found
     */
    public List<ActionHistoryEvent> filterEvents(String actionId) {
        List<ActionHistoryEvent> list = new ArrayList<>();

        // iterate backwards through list (need to find initial event first)
        Iterator<ActionHistoryEvent> iter = actionEvents.descendingIterator();
        long initialId = -1;
        while (iter.hasNext()) {
            ActionHistoryEvent event = iter.next();
            if (event.isInitialEvent() && event.getActionId().equals(actionId)) {
                initialId = event.getEventId();
                list.add(event);
            } else if (initialId == event.getInitialEventId()) {
                list.add(event);
            }
        }
        Collections.reverse(list);
        return list;
    }

    public List<ActionHistoryEvent> filterRetryTimerStartedEvents(String actionId) {
        List<ActionHistoryEvent> actionHistoryEvents = filterEvents(actionId);
        List<ActionHistoryEvent> list = new ArrayList<>();

        for (ActionHistoryEvent event : actionHistoryEvents) {
            if (TimerStarted == event.getType()
                && RETRY_CONTROL_VALUE.equals(event.getHistoryEvent().getTimerStartedEventAttributes().getControl())) {
                list.add(event);
            }
        }
        return list;
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
     * @throws java.lang.UnsupportedOperationException if workflow input is unavailable
     */
    public String getWorkflowInput() {
        if (workflowExecutionStarted == null) {
            throw new UnsupportedOperationException("Workflow input unavailable");
        } else {
            return workflowExecutionStarted.getWorkflowExecutionStartedEventAttributes().getInput();
        }
    }

    public List<HistoryEvent> getErrorEvents() {
        return errorEvents;
    }
}
