package com.clario.swift;

import com.amazonaws.services.redshift.model.UnsupportedOptionException;
import com.amazonaws.services.simpleworkflow.model.*;
import com.clario.swift.action.ActionState;
import org.joda.time.DateTime;

import java.util.HashMap;
import java.util.Map;

import static com.clario.swift.ActionHistoryEvent.EventField.*;
import static com.clario.swift.action.ActionState.*;

/**
 * Class that unifies access to {@link HistoryEvent}s related to Activity, Timer, Child Workflow, or External Signal activities.
 * <p/>
 * Basically trying to extract all the ugliness of Amazon's SWF model into one place so that this API can be cleaner.
 *
 * @author George Coller
 * @see WorkflowHistory
 */
public class ActionHistoryEvent implements Comparable<ActionHistoryEvent> {

    private final HistoryEvent historyEvent;
    private final EventType eventType;
    private final Map<EventField, String> fields;

    /**
     * Construct using an SWF <code>HistoryEvent</code>.
     * Use {@link ActionHistoryEvent#isActionHistoryEvent} to determine if a given <code>HistoryEvent</code> is allowed.
     *
     * @param historyEvent must be compatible with action event
     *
     * @see #isActionHistoryEvent(HistoryEvent)
     */
    public ActionHistoryEvent(HistoryEvent historyEvent) {
        fields = mapFields(historyEvent);

        if (fields.containsKey(state)) {
            this.historyEvent = historyEvent;
            this.eventType = EventType.valueOf(historyEvent.getEventType());
//            this.isInitialEvent = isInitialEventType(eventType);
//            this.initialEventId = findInitialEventId(historyEvent);
        } else {
            throw new IllegalArgumentException("HistoryEvent type is not allowable: " + historyEvent);
        }
    }

    /**
     * Determine if a {@link HistoryEvent} has an SWF {@link EventType} that can be constructed as a <code>ActionEvent</code>.
     */
    public static boolean isActionHistoryEvent(HistoryEvent historyEvent) {
        return findActionState(historyEvent) != null;
    }

    public HistoryEvent getHistoryEvent() {
        return historyEvent;
    }

    /**
     * Initial action events have an {@link #getType()} that starts an activity, timer, or child workflow decision.
     * Clients can use this to check if {@link #getActionId()} is available for this instance.
     *
     * @return true if action type is an initial action event
     * @see #getActionId()
     */
    public boolean isInitialEvent() {
        return fields.containsKey(actionId);
    }

    /**
     * @return unique action identifier for an initiator action event.
     * @throws UnsupportedOperationException if instance is not an initiator action event.
     * @see #isInitialEvent()
     */
    public String getActionId() {
        if (isInitialEvent()) {
            return fields.get(actionId);
        } else {
            throw new UnsupportedOperationException("Cannot get action id on non-initial action event: " + this);
        }
    }

    /**
     * @return {@link HistoryEvent#eventType} cast as {@link EventType} enumeration
     */
    public EventType getType() { return eventType; }

    /**
     * Find the {@link com.clario.swift.action.ActionState} mapping for this history event.
     */
    public ActionState getActionState() { return findActionState(historyEvent); }

    /**
     * @return {@link HistoryEvent#eventId}
     */
    public Long getEventId() { return historyEvent.getEventId(); }

    /**
     * @return {@link HistoryEvent#eventTimestamp}
     */
    public DateTime getEventTimestamp() { return new DateTime(historyEvent.getEventTimestamp()); }

    /**
     * Return the initial event id of the wrapped {@link HistoryEvent}.
     * <p/>
     * If this event is an initial event, return its event id
     * otherwise return it's pointer to the initial history event id
     */
    public Long getInitialEventId() { return Long.valueOf(fields.get(initialEventId)); }

    /**
     * Return the result for actions that have a result-producing {@link HistoryEvent}.
     *
     * @return result or null if not available
     * @see ActionHistoryEvent#findResult
     */
    public String getResult() { return findResult(historyEvent); }

    public String getErrorReason() {
        String reason = findErrorReason(historyEvent);
        if (reason == null) {
            throw new UnsupportedOptionException("Error reason not available for action: " + this);
        }
        return reason;
    }

    //
    // Methods used to convert Amazon objects to Swift.
    //

    /**
     * @return map an EventType to an action state.
     */
    static ActionState findActionState(HistoryEvent historyEvent) {
        String value = mapFields(historyEvent).get(state);
        return value == null ? null : ActionState.valueOf(value);
    }

    /**
     * Unify result-producing {@link HistoryEvent}s by returning their output or null.
     *
     * @see EventType#ActivityTaskCompleted output of activity
     * @see EventType#ChildWorkflowExecutionCompleted output of child workflow
     * @see EventType#ChildWorkflowExecutionStarted runId of started child workflow
     */
    static String findResult(HistoryEvent historyEvent) {
        return mapFields(historyEvent).get(result);
    }

    static String findErrorReason(HistoryEvent historyEvent) {
        Map<EventField, String> map = mapFields(historyEvent);
        String reason = map.get(EventField.reason);
        String details = map.get(EventField.details);
        if (reason != null) {
            return details == null ? reason : reason + " | " + details;
        } else {
            return null;
        }
    }

    enum EventField {actionId, state, initialEventId, input, control, result, reason, details, runId}

    /**
     * Unifies the fields of different SWF history events.
     *
     * @param historyEvent the event to map
     *
     * @return map of field values
     */
    static Map<EventField, String> mapFields(HistoryEvent historyEvent) {
        Map<EventField, String> map = new HashMap<>();
        switch (EventType.valueOf(historyEvent.getEventType())) {

            // Activity Tasks
            case ActivityTaskScheduled:
                map.put(state, active.name());
                map.put(initialEventId, historyEvent.getEventId().toString());
                ActivityTaskScheduledEventAttributes activityScheduled = historyEvent.getActivityTaskScheduledEventAttributes();
                map.put(actionId, activityScheduled.getActivityId());
                map.put(input, activityScheduled.getInput());
                map.put(control, activityScheduled.getControl());
                break;
            case ActivityTaskStarted:
                map.put(state, active.name());
                map.put(initialEventId, historyEvent.getActivityTaskStartedEventAttributes().getScheduledEventId().toString());
                break;
            case ActivityTaskCompleted:
                map.put(state, success.name());
                ActivityTaskCompletedEventAttributes activityCompleted = historyEvent.getActivityTaskCompletedEventAttributes();
                map.put(initialEventId, activityCompleted.getScheduledEventId().toString());
                map.put(result, activityCompleted.getResult());
                break;
            case ActivityTaskCanceled:
                map.put(state, error.name());
                ActivityTaskCanceledEventAttributes activityCanceled = historyEvent.getActivityTaskCanceledEventAttributes();
                map.put(initialEventId, activityCanceled.getScheduledEventId().toString());
                map.put(details, activityCanceled.getDetails());
                break;
            case ActivityTaskFailed:
                map.put(state, error.name());
                ActivityTaskFailedEventAttributes activityFailed = historyEvent.getActivityTaskFailedEventAttributes();
                map.put(initialEventId, activityFailed.getScheduledEventId().toString());
                map.put(reason, activityFailed.getReason());
                map.put(details, activityFailed.getDetails());
                break;
            case ActivityTaskTimedOut:
                map.put(state, error.name());
                ActivityTaskTimedOutEventAttributes activityTimedOut = historyEvent.getActivityTaskTimedOutEventAttributes();
                map.put(initialEventId, activityTimedOut.getScheduledEventId().toString());
                map.put(reason, activityTimedOut.getTimeoutType());
                map.put(details, activityTimedOut.getDetails());
                break;

            // Timers
            case TimerStarted:
                map.put(state, active.name());
                map.put(initialEventId, historyEvent.getEventId().toString());
                TimerStartedEventAttributes timerStarted = historyEvent.getTimerStartedEventAttributes();
                map.put(actionId, timerStarted.getTimerId());
                map.put(control, timerStarted.getControl());
                break;
            case TimerFired:
                // NOTE: This could be a 'retry' event but we don't have the control field handy yet.
                map.put(state, success.name());
                map.put(initialEventId, historyEvent.getTimerFiredEventAttributes().getStartedEventId().toString());
                break;
            case StartTimerFailed:
                map.put(state, error.name());
                map.put(reason, historyEvent.getStartTimerFailedEventAttributes().getCause());
                break;
            case TimerCanceled:
                map.put(state, error.name());
                map.put(reason, "timer canceled");
                break;

            // Child Workflows
            case StartChildWorkflowExecutionInitiated:
                map.put(state, active.name());
                map.put(initialEventId, historyEvent.getEventId().toString());
                StartChildWorkflowExecutionInitiatedEventAttributes childInitiated = historyEvent.getStartChildWorkflowExecutionInitiatedEventAttributes();
                map.put(actionId, childInitiated.getWorkflowId());
                map.put(input, childInitiated.getInput());
                map.put(control, childInitiated.getControl());
                break;
            case ChildWorkflowExecutionStarted:
                map.put(state, active.name());
                ChildWorkflowExecutionStartedEventAttributes childStarted = historyEvent.getChildWorkflowExecutionStartedEventAttributes();
                map.put(initialEventId, childStarted.getInitiatedEventId().toString());
                map.put(runId, childStarted.getWorkflowExecution().getRunId());
                map.put(result, childStarted.getWorkflowExecution().getRunId());
                break;
            case ChildWorkflowExecutionCompleted:
                map.put(state, success.name());
                map.put(initialEventId, historyEvent.getChildWorkflowExecutionCompletedEventAttributes().getInitiatedEventId().toString());
                map.put(result, historyEvent.getChildWorkflowExecutionCompletedEventAttributes().getResult());
                break;
            case ChildWorkflowExecutionCanceled:
                map.put(state, error.name());
                ChildWorkflowExecutionCanceledEventAttributes childCanceled = historyEvent.getChildWorkflowExecutionCanceledEventAttributes();
                map.put(initialEventId, childCanceled.getInitiatedEventId().toString());
                map.put(reason, childCanceled.getDetails());
                break;
            case ChildWorkflowExecutionFailed:
                map.put(state, error.name());
                ChildWorkflowExecutionFailedEventAttributes childFailed = historyEvent.getChildWorkflowExecutionFailedEventAttributes();
                map.put(initialEventId, childFailed.getInitiatedEventId().toString());
                map.put(reason, childFailed.getReason());
                map.put(details, childFailed.getDetails());
                break;
            case ChildWorkflowExecutionTerminated:
                map.put(state, error.name());
                map.put(initialEventId, historyEvent.getChildWorkflowExecutionTerminatedEventAttributes().getInitiatedEventId().toString());
                map.put(reason, "child workflow terminated");
                break;
            case ChildWorkflowExecutionTimedOut:
                map.put(state, error.name());
                ChildWorkflowExecutionTimedOutEventAttributes childTimedOut = historyEvent.getChildWorkflowExecutionTimedOutEventAttributes();
                map.put(initialEventId, childTimedOut.getInitiatedEventId().toString());
                map.put(reason, childTimedOut.getTimeoutType());
                break;

            // Signals
            case SignalExternalWorkflowExecutionInitiated:
                map.put(state, active.name());
                map.put(initialEventId, historyEvent.getEventId().toString());
                SignalExternalWorkflowExecutionInitiatedEventAttributes signalInitiated = historyEvent.getSignalExternalWorkflowExecutionInitiatedEventAttributes();
                map.put(actionId, signalInitiated.getSignalName());
                map.put(input, signalInitiated.getInput());
                map.put(control, signalInitiated.getControl());
                break;
            case ExternalWorkflowExecutionSignaled:
                map.put(state, success.name());
                ExternalWorkflowExecutionSignaledEventAttributes signalSignaled = historyEvent.getExternalWorkflowExecutionSignaledEventAttributes();
                map.put(initialEventId, signalSignaled.getInitiatedEventId().toString());
                map.put(runId, signalSignaled.getWorkflowExecution().getRunId());
                break;

            // Markers
            case MarkerRecorded:
                map.put(state, success.name());
                map.put(initialEventId, historyEvent.getEventId().toString());
                MarkerRecordedEventAttributes markerRecorded = historyEvent.getMarkerRecordedEventAttributes();
                map.put(actionId, markerRecorded.getMarkerName());
                map.put(input, markerRecorded.getDetails());
                break;
            default:
        }
        // no null values
        for (EventField field : EventField.values()) {
            if (map.containsKey(field) && map.get(field) == null) {
                map.put(field, "");
            }
        }
        return map;
    }

    /**
     * Two events are consider equal if they share the same {@link #getEventId()}.
     */
    public boolean equals(Object o) {
        return this == o || o instanceof ActionHistoryEvent && getEventId().equals(((ActionHistoryEvent) o).getEventId());
    }

    public int hashCode() {
        return getEventId().hashCode();
    }

    @Override
    public String toString() {
        return SwiftUtil.DATE_TIME_FORMATTER.print(getEventTimestamp())
            + ' ' + getType()
            + ' ' + getEventId()
            + (isInitialEvent() ? " " + getActionId() : " -> " + getInitialEventId());
    }

    @Override
    public int compareTo(ActionHistoryEvent event) {
        return -getEventId().compareTo(event.getEventId());
    }
}
