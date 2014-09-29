package com.clario.swift;

import com.amazonaws.services.simpleworkflow.model.*;
import com.clario.swift.action.Action;
import com.clario.swift.action.ActionState;
import org.joda.time.DateTime;

import java.util.EnumMap;
import java.util.Map;

import static com.clario.swift.ActionEvent.EventField.*;
import static com.clario.swift.action.ActionState.*;

/**
 * Wrap SWF {@link HistoryEvent} instances to provide a uniform API across event types.
 * </p>
 * An instance will be assigned {@link ActionState#undefined} if the wrapped {@link HistoryEvent} does not
 * belong to an {@link Action}: Activity, Timer, Marker, Signal, Start Child, ContinueAsNew.
 *
 * @author George Coller
 */
public class ActionEvent implements Comparable<ActionEvent> {
    private final HistoryEvent historyEvent;
    private final Map<EventField, Object> fields;

    /**
     * Construct instance with an {@link HistoryEvent}.
     */
    public ActionEvent(HistoryEvent historyEvent) {
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
     * Initial events relate to the initial decision to start an  an activity, timer, marker, signal, child workflow, etc.
     *
     * @return true if action type is an initial action event
     * @see #getActionId
     */
    public boolean isInitialEvent() {
        return Boolean.TRUE == fields.get(isInitialEvent);
    }

    /**
     * Unique identifier for an initiated {@link Action}.
     * <p/>
     * Only available on initial events, so use {@link #isInitialEvent()} before calling this method.
     *
     * @return unique action identifier related to this event
     * @throws UnsupportedOperationException if instance is not an initiator action event.
     * @see #isInitialEvent
     */
    public String getActionId() {
        return getField(actionId, true);
    }

    /**
     * @return {@link HistoryEvent#eventType} cast to an {@link EventType} enumeration value.
     */
    public EventType getType() { return getField(eventType, true); }

    /**
     * @return action state for this event
     */
    public ActionState getActionState() { return getField(actionState, true); }

    /**
     * Event ids are unique within a workflow history and are created in sequence.
     *
     * @see HistoryEvent#eventId
     */
    public Long getEventId() { return getField(eventId, true); }

    /**
     * @see HistoryEvent#eventTimestamp
     */
    public DateTime getEventTimestamp() { return getField(timestamp, true); }

    /**
     * An 'initial event' is one that is related to the decision that started a particular {@link Action}.
     *
     * @return the initial event id of initial event, or if this is an initial event, returns {@link #getEventId}.
     */
    public Long getInitialEventId() { return getField(initialEventId, true); }

    /**
     * Access primary data field for this event.
     * <p/>
     * Each SWF event type may have one or several 'data' fields, for example the field 'input' on
     * an event with type <code>ActivityTaskScheduled</code> contains the input for that activity.
     * <p/>
     * The following table lists which data fields are returned by this method and {@link #getData2}:
     * <pre>{@code &nbsp;
     * Field                                    | Data 1      | Data 2
     * -----------------------------------------+-------------+--------
     * ActivityTaskScheduled                    | input       | control
     * ActivityTaskStarted                      | identity    | -
     * ActivityTaskCompleted                    | result      | -
     * ActivityTaskCanceled                     | details     | -
     * ActivityTaskFailed                       | reason      | details
     * ActivityTaskTimedOut                     | timeoutType | details
     * TimerStarted                             | control     | -
     * TimerFired                               | -           | -
     * TimerCanceled                            | _           | -
     * StartTimerFailed                         | reason      | -
     * StartChildWorkflowExecutionInitiated     | input       | control
     * ChildWorkflowExecutionStarted            | runId       | -
     * ChildWorkflowExecutionCompleted          | result      | -
     * ChildWorkflowExecutionCanceled           | reason      | -
     * ChildWorkflowExecutionFailed             | reason      | details
     * ChildWorkflowExecutionTerminated         | reason      | -
     * ChildWorkflowExecutionTimedOut           | timeoutType | -
     * SignalExternalWorkflowExecutionInitiated | input       | control
     * ExternalWorkflowExecutionSignaled        | runId       | -
     * WorkflowExecutionSignaled                | input       | -
     * MarkerRecorded                           | details     | -
     * DecisionTaskCompleted                    | context     | _
     * }</pre>
     *
     * @return the primary data field for this event or null if it does not exist.
     * @see HistoryEvent
     */
    public String getData1() {
        return getField(dataValue1, false);
    }

    /**
     * @return the secondary data field for this event or null if it does not exist.
     * See {@link #getData1} for details.
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
        eventId, eventType, isInitialEvent, timestamp, actionId, actionState, initialEventId, dataField1, dataValue1, dataField2, dataValue2
    }

    /**
     * Parses out fields in the given {@link HistoryEvent} into a field map, which provides more uniform access.
     *
     * @param historyEvent the event to map
     *
     * @return map of field values
     */
    static Map<EventField, Object> mapFields(HistoryEvent historyEvent) {
        Map<EventField, Object> map = new EnumMap<EventField, Object>(EventField.class);
        map.put(timestamp, new DateTime(historyEvent.getEventTimestamp()));
        map.put(eventId, historyEvent.getEventId());
        EventType type = EventType.valueOf(historyEvent.getEventType());
        map.put(eventType, type);

        switch (type) {
            // Activity Tasks
            case ActivityTaskScheduled:
                map.put(actionState, active);
                map.put(initialEventId, historyEvent.getEventId());
                map.put(isInitialEvent, true);
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
                map.put(isInitialEvent, true);
                TimerStartedEventAttributes timerStarted = historyEvent.getTimerStartedEventAttributes();
                map.put(actionId, timerStarted.getTimerId());
                map.put(dataField1, "control");
                map.put(dataValue1, timerStarted.getControl());
                break;
            case TimerFired:
                // If this is a retry timer event the actionState is really 'retry' but we don't have the related TimerStarted.control field yet
                map.put(actionState, success);
                TimerFiredEventAttributes timerFired = historyEvent.getTimerFiredEventAttributes();
                map.put(actionId, timerFired.getTimerId());
                map.put(initialEventId, timerFired.getStartedEventId());
                break;
            case TimerCanceled:
                // If this is a retry timer event the actionState is really 'retry' but we don't have the related TimerStarted.control field yet
                map.put(actionState, success);
                TimerCanceledEventAttributes timerCanceled = historyEvent.getTimerCanceledEventAttributes();
                map.put(actionId, timerCanceled.getTimerId());
                map.put(initialEventId, timerCanceled.getStartedEventId());
                break;
            case StartTimerFailed:
                map.put(actionState, error);
                map.put(dataField1, "reason");
                map.put(dataValue1, historyEvent.getStartTimerFailedEventAttributes().getCause());
                break;

            // Child Workflows
            case StartChildWorkflowExecutionInitiated:
                map.put(actionState, active);
                map.put(initialEventId, historyEvent.getEventId());
                map.put(isInitialEvent, true);
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
                map.put(isInitialEvent, true);
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
                map.put(isInitialEvent, true);
                WorkflowExecutionSignaledEventAttributes signaled = historyEvent.getWorkflowExecutionSignaledEventAttributes();
                map.put(actionId, signaled.getSignalName());
                map.put(dataField1, "input");
                map.put(dataValue1, signaled.getInput());
                break;

            // Markers
            case MarkerRecorded:
                map.put(actionState, success);
                map.put(initialEventId, historyEvent.getEventId());
                map.put(isInitialEvent, true);
                MarkerRecordedEventAttributes markerRecorded = historyEvent.getMarkerRecordedEventAttributes();
                map.put(actionId, markerRecorded.getMarkerName());
                map.put(dataField1, "details");
                map.put(dataValue1, markerRecorded.getDetails());
                break;

            // Decision Task Completed
            case DecisionTaskCompleted:
                map.put(actionState, success);
                map.put(isInitialEvent, false);
                map.put(initialEventId, historyEvent.getDecisionTaskCompletedEventAttributes().getScheduledEventId());
                map.put(dataField1, "context");
                map.put(dataValue1, historyEvent.getDecisionTaskCompletedEventAttributes().getExecutionContext());
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
        return this == o || o instanceof ActionEvent && getEventId().equals(((ActionEvent) o).getEventId());
    }

    public int hashCode() {
        return getEventId().hashCode();
    }

    @Override
    public String toString() {
        return fields.toString();
    }

    /**
     * Sort by eventId descending.
     */
    public int compareTo(ActionEvent event) {
        return -getEventId().compareTo(event.getEventId());
    }
}
