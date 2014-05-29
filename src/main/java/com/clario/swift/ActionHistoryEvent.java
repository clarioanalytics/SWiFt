package com.clario.swift;

import com.amazonaws.services.simpleworkflow.model.*;
import com.clario.swift.action.ActionState;
import org.joda.time.DateTime;

import java.util.EnumMap;
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
    private final Map<EventField, Object> fields;

    /**
     * Construct using an SWF <code>HistoryEvent</code>.
     *
     * @param historyEvent must be compatible with action event
     */
    public ActionHistoryEvent(HistoryEvent historyEvent) {
        fields = mapFields(historyEvent);
        this.historyEvent = historyEvent;
    }

    /**
     * @return wrapped SWF history event.
     */
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
        return getField(actionId, true);
    }

    /**
     * @return {@link HistoryEvent#eventType} cast as {@link EventType} enumeration
     */
    public EventType getType() { return getField(eventType, true); }

    /**
     * Find the {@link ActionState} mapping for this history event.
     */
    public ActionState getActionState() { return getField(actionState, true); }

    /**
     * @return {@link HistoryEvent#eventId}
     */
    public Long getEventId() { return getField(eventId, true); }

    /**
     * @return {@link HistoryEvent#eventTimestamp}
     */
    public DateTime getEventTimestamp() { return getField(timestamp, true); }

    /**
     * Return the initial event id of the wrapped {@link HistoryEvent}.
     * <p/>
     * If this event is an initial event, return its event id
     * otherwise return it's pointer to the initial history event id
     */
    public Long getInitialEventId() { return getField(initialEventId, true); }

    /**
     * @return the primary data field for this event or null if it does not exist.
     */
    public String getData() {
        return getField(dataValue1, false);
    }

    /**
     * @return the secondary data field for this event or null if it does not exist.
     */
    public String getData2() {
        return getField(dataValue2, false);
    }

    /**
     * Generic accessor for {@link EventField}s mapped from this instance's {@link HistoryEvent}.
     *
     * @param field field to get
     * @param isRequired if true, throw an UnsupportedOperationException if the field value is null
     * @param <T> cast field as
     *
     * @return the field value
     * @throws UnsupportedOperationException if the field is null and isRequired parameter is true
     */
    @SuppressWarnings("unchecked")
    public <T> T getField(EventField field, boolean isRequired) {
        T value = (T) fields.get(field);
        if (isRequired && value == null) {
            throw new UnsupportedOperationException(field + " not available for action: " + this);
        }
        return value;
    }

    public enum EventField {
        eventId, eventType, timestamp, actionId, actionState, initialEventId, dataField1, dataValue1, dataField2, dataValue2
    }

    /**
     * Unifies the fields of different SWF history events.
     *
     * @param historyEvent the event to map
     *
     * @return map of field values
     */
    static Map<EventField, Object> mapFields(HistoryEvent historyEvent) {
        Map<EventField, Object> map = new EnumMap<>(EventField.class);
        map.put(timestamp, new DateTime(historyEvent.getEventTimestamp()));
        map.put(eventId, historyEvent.getEventId());
        EventType type = EventType.valueOf(historyEvent.getEventType());
        map.put(eventType, type);

        switch (type) {
            // Activity Tasks
            case ActivityTaskScheduled:
                map.put(actionState, active);
                map.put(initialEventId, historyEvent.getEventId());
                ActivityTaskScheduledEventAttributes activityScheduled = historyEvent.getActivityTaskScheduledEventAttributes();
                map.put(actionId, activityScheduled.getActivityId());
                map.put(dataField1, "input");
                map.put(dataValue1, activityScheduled.getInput());
                map.put(dataField2, "control");
                map.put(dataValue2, activityScheduled.getControl());
                break;
            case ActivityTaskStarted:
                map.put(actionState, active);
                map.put(initialEventId, historyEvent.getActivityTaskStartedEventAttributes().getScheduledEventId());
                map.put(dataField1, "identity");
                map.put(dataValue1, historyEvent.getActivityTaskStartedEventAttributes().getIdentity());
                break;
            case ActivityTaskCompleted:
                map.put(actionState, success);
                ActivityTaskCompletedEventAttributes activityCompleted = historyEvent.getActivityTaskCompletedEventAttributes();
                map.put(initialEventId, activityCompleted.getScheduledEventId());
                map.put(dataField1, "result");
                map.put(dataValue1, activityCompleted.getResult());
                break;
            case ActivityTaskCanceled:
                map.put(actionState, error);
                ActivityTaskCanceledEventAttributes activityCanceled = historyEvent.getActivityTaskCanceledEventAttributes();
                map.put(initialEventId, activityCanceled.getScheduledEventId());
                map.put(dataField1, "details");
                map.put(dataValue1, activityCanceled.getDetails());
                break;
            case ActivityTaskFailed:
                map.put(actionState, error);
                ActivityTaskFailedEventAttributes activityFailed = historyEvent.getActivityTaskFailedEventAttributes();
                map.put(initialEventId, activityFailed.getScheduledEventId());
                map.put(dataField1, "reason");
                map.put(dataValue1, activityFailed.getReason());
                map.put(dataField2, "details");
                map.put(dataValue2, activityFailed.getDetails());
                break;
            case ActivityTaskTimedOut:
                map.put(actionState, error);
                ActivityTaskTimedOutEventAttributes activityTimedOut = historyEvent.getActivityTaskTimedOutEventAttributes();
                map.put(initialEventId, activityTimedOut.getScheduledEventId());
                map.put(dataField1, "timeoutType");
                map.put(dataValue1, activityTimedOut.getTimeoutType());
                map.put(dataField2, "details");
                map.put(dataValue2, activityTimedOut.getDetails());
                break;

            // Timers
            case TimerStarted:
                map.put(actionState, active);
                map.put(initialEventId, historyEvent.getEventId());
                TimerStartedEventAttributes timerStarted = historyEvent.getTimerStartedEventAttributes();
                map.put(actionId, timerStarted.getTimerId());
                map.put(dataField1, "control");
                map.put(dataValue1, timerStarted.getControl());
                break;
            case TimerFired:
                // NOTE: This could be a 'retry' event but we don't have the control field yet from the related TimerStarted event.
                map.put(actionState, success);
                TimerFiredEventAttributes timerFired = historyEvent.getTimerFiredEventAttributes();
                map.put(actionId, timerFired.getTimerId());
                map.put(initialEventId, timerFired.getStartedEventId());
                break;
            case StartTimerFailed:
                map.put(actionState, error);
                map.put(dataField1, "reason");
                map.put(dataValue1, historyEvent.getStartTimerFailedEventAttributes().getCause());
                break;
            case TimerCanceled:
                map.put(actionState, error);
                map.put(dataField1, "reason");
                map.put(dataValue1, "timer canceled");
                break;

            // Child Workflows
            case StartChildWorkflowExecutionInitiated:
                map.put(actionState, active);
                map.put(initialEventId, historyEvent.getEventId());
                StartChildWorkflowExecutionInitiatedEventAttributes childInitiated = historyEvent.getStartChildWorkflowExecutionInitiatedEventAttributes();
                map.put(actionId, childInitiated.getWorkflowId());
                map.put(dataField2, "input");
                map.put(dataValue1, childInitiated.getInput());
                map.put(dataField2, "control");
                map.put(dataValue2, childInitiated.getControl());
                break;
            case ChildWorkflowExecutionStarted:
                map.put(actionState, active);
                ChildWorkflowExecutionStartedEventAttributes childStarted = historyEvent.getChildWorkflowExecutionStartedEventAttributes();
                map.put(initialEventId, childStarted.getInitiatedEventId());
                map.put(dataField1, "runId");
                map.put(dataValue1, childStarted.getWorkflowExecution().getRunId());
                break;
            case ChildWorkflowExecutionCompleted:
                map.put(actionState, success);
                map.put(initialEventId, historyEvent.getChildWorkflowExecutionCompletedEventAttributes().getInitiatedEventId());
                map.put(dataField1, "result");
                map.put(dataValue1, historyEvent.getChildWorkflowExecutionCompletedEventAttributes().getResult());
                break;
            case ChildWorkflowExecutionCanceled:
                map.put(actionState, error);
                ChildWorkflowExecutionCanceledEventAttributes childCanceled = historyEvent.getChildWorkflowExecutionCanceledEventAttributes();
                map.put(initialEventId, childCanceled.getInitiatedEventId());
                map.put(dataField1, "reason");
                map.put(dataValue1, childCanceled.getDetails());
                break;
            case ChildWorkflowExecutionFailed:
                map.put(actionState, error);
                ChildWorkflowExecutionFailedEventAttributes childFailed = historyEvent.getChildWorkflowExecutionFailedEventAttributes();
                map.put(initialEventId, childFailed.getInitiatedEventId());
                map.put(dataField1, "reason");
                map.put(dataValue1, childFailed.getReason());
                map.put(dataField1, "details");
                map.put(dataValue2, childFailed.getDetails());
                break;
            case ChildWorkflowExecutionTerminated:
                map.put(actionState, error);
                map.put(initialEventId, historyEvent.getChildWorkflowExecutionTerminatedEventAttributes().getInitiatedEventId());
                map.put(dataField1, "reason");
                map.put(dataValue1, "child workflow terminated");
                break;
            case ChildWorkflowExecutionTimedOut:
                map.put(actionState, error);
                ChildWorkflowExecutionTimedOutEventAttributes childTimedOut = historyEvent.getChildWorkflowExecutionTimedOutEventAttributes();
                map.put(initialEventId, childTimedOut.getInitiatedEventId());
                map.put(dataField1, "timeoutType");
                map.put(dataValue1, childTimedOut.getTimeoutType());
                break;

            // Signal External Workflows
            case SignalExternalWorkflowExecutionInitiated:
                map.put(actionState, active);
                map.put(initialEventId, historyEvent.getEventId());
                SignalExternalWorkflowExecutionInitiatedEventAttributes signalInitiated = historyEvent.getSignalExternalWorkflowExecutionInitiatedEventAttributes();
                map.put(actionId, signalInitiated.getSignalName());
                map.put(dataField1, "input");
                map.put(dataValue1, signalInitiated.getInput());
                map.put(dataField2, "control");
                map.put(dataValue2, signalInitiated.getControl());
                break;
            case ExternalWorkflowExecutionSignaled:
                map.put(actionState, success);
                ExternalWorkflowExecutionSignaledEventAttributes signalSignaled = historyEvent.getExternalWorkflowExecutionSignaledEventAttributes();
                map.put(initialEventId, signalSignaled.getInitiatedEventId());
                map.put(dataField1, "runId");
                map.put(dataValue1, signalSignaled.getWorkflowExecution().getRunId());
                break;

            // Signal Received (either from this workflow or external source)
            case WorkflowExecutionSignaled:
                map.put(actionState, success);
                map.put(initialEventId, historyEvent.getEventId());
                WorkflowExecutionSignaledEventAttributes signaled = historyEvent.getWorkflowExecutionSignaledEventAttributes();
                map.put(actionId, signaled.getSignalName());
                map.put(dataField1, "input");
                map.put(dataValue1, signaled.getInput());
                break;

            // Markers
            case MarkerRecorded:
                map.put(actionState, success);
                map.put(initialEventId, historyEvent.getEventId());
                MarkerRecordedEventAttributes markerRecorded = historyEvent.getMarkerRecordedEventAttributes();
                map.put(actionId, markerRecorded.getMarkerName());
                map.put(dataField1, "details");
                map.put(dataValue1, markerRecorded.getDetails());
                break;
            default:
                map.put(actionState, ActionState.undefined);
        }
        // replace any null data values with empty string;
        if (map.containsKey(dataValue1) && map.get(dataValue1) == null) { map.put(dataValue1, ""); }
        if (map.containsKey(dataValue2) && map.get(dataValue2) == null) { map.put(dataValue2, ""); }
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
        return fields.toString();
    }

    @Override
    public int compareTo(ActionHistoryEvent event) {
        return -getEventId().compareTo(event.getEventId());
    }
}
