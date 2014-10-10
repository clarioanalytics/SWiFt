package com.clario.swift;

import com.amazonaws.services.simpleworkflow.model.*;
import com.clario.swift.action.Action;
import com.clario.swift.action.TimerAction;
import org.joda.time.DateTime;

import java.util.EnumMap;
import java.util.Map;

import static com.clario.swift.Event.Field.*;
import static com.clario.swift.Event.State.*;

/**
 * Wraps an SWF {@link HistoryEvent} to provide and uniform API for selecting and working with events.
 * <p/>
 * Fields are mapped from the given {@link HistoryEvent} so are immutable even the wrapped class is altered.
 *
 * @author George Coller
 */
public class Event implements Comparable<Event> {

    /**
     * Tasks in SWF are represented by one or more related {@link Event} in history with the most recent event
     * representing the current state of the task.
     * <p/>
     * This enumeration makes working with state across different task types easier.
     */
    public static enum State {
        /** State representing an {@link Action} that has not been scheduled (has no history events). */
        INITIAL,

        /** History event representing an {@link Action} that has started and is not yet complete. */
        ACTIVE,

        /** History event representing an {@link Action} that should be re-scheduled. */
        RETRY,

        /** History event representing an {@link Action} that finished in a successful state. */
        SUCCESS,

        /** History event representing an {@link Action} that finished in an error state. */
        ERROR,

        /** History event representing an error on SWF that will immediately fail the workflow */
        CRITICAL,

        /** Informational history event that, not usually useful in decision making */
        INFO
    }

    /**
     * Enumeration of {@link HistoryEvent} fields mapped to each {@link Event}.
     * <p/>
     * Like {@link State}, {@link Field} unifies concepts across different {@link HistoryEvent} types
     * to make working with them more uniform and easy.
     */
    public static enum Field {
        eventId, eventType, timestamp, actionId, state, initialEventId, dataField1, dataValue1, dataField2, dataValue2, isInitialAction
    }

    private final HistoryEvent historyEvent;
    private final Map<Field, Object> fieldValueMap;

    /**
     * Construct an immutable instance wrapping a {@link HistoryEvent}.
     */
    public Event(HistoryEvent historyEvent) {
        fieldValueMap = mapFields(historyEvent);
        this.historyEvent = historyEvent;
    }

    /**
     * @return the wrapped SWF history event.
     */
    public HistoryEvent getHistoryEvent() {
        return historyEvent;
    }

    /**
     * Initial actions relate to the initial decision to start an {@link Action} subclass:
     * activity, timer, marker, signal, child workflow, etc.
     *
     * @return true if is an initial action.
     * @see #getActionId
     */
    public boolean isInitialAction() {
        return Boolean.TRUE == fieldValueMap.get(isInitialAction);
    }

    /**
     * Unique identifier for an initiated {@link Action}.
     * <p/>
     * Only available on initial action history events, so use {@link #isInitialAction()} before calling this method if unsure.
     *
     * @return unique action identifier related to this event
     * @throws UnsupportedOperationException if instance is not an initiator action event.
     * @see #isInitialAction
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
    public State getActionState() { return getField(state, true); }

    /**
     * Event ids are unique within a workflow history and represent the sequence of events.
     *
     * @see HistoryEvent#eventId
     */
    public Long getEventId() { return getField(eventId, true); }

    /**
     * Timestamp of event.
     *
     * @see HistoryEvent#eventTimestamp
     */
    public DateTime getEventTimestamp() { return getField(timestamp, true); }

    /**
     * Return the event id of the first event for this event group.
     * <p/>
     * An event group represents a series of events that represent a single unit of work in SWF. For example a successful decision
     * is composed of three events with types: {@code DecisionTaskScheduled, DecisionTaskStarted, DecisionTaskCompleted}.  A
     * successful {@link TimerAction} would be composed of two events with types: {@code TimerStarted, TimerFired}.
     * <p/>
     * {@link #getInitialEventId()} will be the same value returned by each member of the group and will be the
     * event id of the first member.
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
     * Events that will have populate data fields:
     * <pre>{@code &nbsp;
     * EventType                                | Data 1      | Data 2
     * -----------------------------------------+-------------+--------
     * ActivityTaskScheduled                    | input       | control
     * ActivityTaskStarted                      | identity    | -
     * ActivityTaskCompleted                    | result      | -
     * ActivityTaskCanceled                     | details     | -
     * ActivityTaskFailed                       | reason      | details
     * ActivityTaskTimedOut                     | timeoutType | details
     * TimerStarted                             | control     | -
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
     * WorkflowExecutionStarted                 | input       | _
     * WorkflowExecutionCancelRequested         | cause       | _
     * ScheduleActivityTaskFailed               | cause       | activityId
     * StartChildWorkflowExecutionFailed        | cause       | control
     * SignalExternalWorkflowExecutionFailed    | cause       | control
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
     * Generic accessor for {@link Field}s mapped from this instance's {@link HistoryEvent}.
     *
     * @param field field to get
     * @param isRequired if true, throw an UnsupportedOperationException if the field value is null
     * @param <T> cast field as
     *
     * @return the field value
     * @throws UnsupportedOperationException if the field is null and isRequired parameter is true
     */
    @SuppressWarnings("unchecked")
    public <T> T getField(Field field, boolean isRequired) {
        T value = (T) fieldValueMap.get(field);
        if (isRequired && value == null) {
            throw new UnsupportedOperationException(field + " not available for action: " + this);
        }
        return value;
    }

    /**
     * Parses out fields in the given {@link HistoryEvent} into a field map, which provides uniform access.
     *
     * @param historyEvent the event to map
     *
     * @return map of field values
     */
    static Map<Field, Object> mapFields(HistoryEvent historyEvent) {
        Map<Field, Object> map = new EnumMap<Field, Object>(Field.class);
        map.put(timestamp, new DateTime(historyEvent.getEventTimestamp()));
        map.put(eventId, historyEvent.getEventId());
        EventType type = EventType.valueOf(historyEvent.getEventType());
        map.put(eventType, type);

        switch (type) {
            // Activity Tasks
            case ActivityTaskScheduled:
                map.put(state, ACTIVE);
                map.put(initialEventId, historyEvent.getEventId());
                map.put(isInitialAction, true);
                ActivityTaskScheduledEventAttributes activityScheduled = historyEvent.getActivityTaskScheduledEventAttributes();
                map.put(actionId, activityScheduled.getActivityId());
                map.put(dataField1, "input");
                map.put(dataValue1, activityScheduled.getInput());
                map.put(dataField2, "control");
                map.put(dataValue2, activityScheduled.getControl());
                break;
            case ActivityTaskStarted:
                map.put(state, ACTIVE);
                map.put(initialEventId, historyEvent.getActivityTaskStartedEventAttributes().getScheduledEventId());
                map.put(dataField1, "identity");
                map.put(dataValue1, historyEvent.getActivityTaskStartedEventAttributes().getIdentity());
                break;
            case ActivityTaskCompleted:
                map.put(state, SUCCESS);
                ActivityTaskCompletedEventAttributes activityCompleted = historyEvent.getActivityTaskCompletedEventAttributes();
                map.put(initialEventId, activityCompleted.getScheduledEventId());
                map.put(dataField1, "result");
                map.put(dataValue1, activityCompleted.getResult());
                break;
            case ActivityTaskCanceled:
                map.put(state, ERROR);
                ActivityTaskCanceledEventAttributes activityCanceled = historyEvent.getActivityTaskCanceledEventAttributes();
                map.put(initialEventId, activityCanceled.getScheduledEventId());
                map.put(dataField1, "details");
                map.put(dataValue1, activityCanceled.getDetails());
                break;
            case ActivityTaskFailed:
                map.put(state, ERROR);
                ActivityTaskFailedEventAttributes activityFailed = historyEvent.getActivityTaskFailedEventAttributes();
                map.put(initialEventId, activityFailed.getScheduledEventId());
                map.put(dataField1, "reason");
                map.put(dataValue1, activityFailed.getReason());
                map.put(dataField2, "details");
                map.put(dataValue2, activityFailed.getDetails());
                break;
            case ActivityTaskTimedOut:
                map.put(state, ERROR);
                ActivityTaskTimedOutEventAttributes activityTimedOut = historyEvent.getActivityTaskTimedOutEventAttributes();
                map.put(initialEventId, activityTimedOut.getScheduledEventId());
                map.put(dataField1, "timeoutType");
                map.put(dataValue1, activityTimedOut.getTimeoutType());
                map.put(dataField2, "details");
                map.put(dataValue2, activityTimedOut.getDetails());
                break;

            // Timers
            case TimerStarted:
                map.put(state, ACTIVE);
                map.put(initialEventId, historyEvent.getEventId());
                map.put(isInitialAction, true);
                TimerStartedEventAttributes timerStarted = historyEvent.getTimerStartedEventAttributes();
                map.put(actionId, timerStarted.getTimerId());
                map.put(dataField1, "control");
                map.put(dataValue1, timerStarted.getControl());
                break;
            case TimerFired:
                // If this is a retry timer event the actionState is really 'retry' but we don't have the related TimerStarted.control field yet
                map.put(state, SUCCESS);
                TimerFiredEventAttributes timerFired = historyEvent.getTimerFiredEventAttributes();
                map.put(actionId, timerFired.getTimerId());
                map.put(initialEventId, timerFired.getStartedEventId());
                break;
            case TimerCanceled:
                // If this is a retry timer event the actionState is really 'retry' but we don't have the related TimerStarted.control field yet
                map.put(state, SUCCESS);
                TimerCanceledEventAttributes timerCanceled = historyEvent.getTimerCanceledEventAttributes();
                map.put(actionId, timerCanceled.getTimerId());
                map.put(initialEventId, timerCanceled.getStartedEventId());
                break;
            case StartTimerFailed:
                map.put(state, ERROR);
                map.put(dataField1, "reason");
                map.put(dataValue1, historyEvent.getStartTimerFailedEventAttributes().getCause());
                break;

            // Child Workflows
            case StartChildWorkflowExecutionInitiated:
                map.put(state, ACTIVE);
                map.put(initialEventId, historyEvent.getEventId());
                map.put(isInitialAction, true);
                StartChildWorkflowExecutionInitiatedEventAttributes childInitiated = historyEvent.getStartChildWorkflowExecutionInitiatedEventAttributes();
                map.put(actionId, childInitiated.getWorkflowId());
                map.put(dataField2, "input");
                map.put(dataValue1, childInitiated.getInput());
                map.put(dataField2, "control");
                map.put(dataValue2, childInitiated.getControl());
                break;
            case ChildWorkflowExecutionStarted:
                map.put(state, ACTIVE);
                ChildWorkflowExecutionStartedEventAttributes childStarted = historyEvent.getChildWorkflowExecutionStartedEventAttributes();
                map.put(initialEventId, childStarted.getInitiatedEventId());
                map.put(dataField1, "runId");
                map.put(dataValue1, childStarted.getWorkflowExecution().getRunId());
                break;
            case ChildWorkflowExecutionCompleted:
                map.put(state, SUCCESS);
                map.put(initialEventId, historyEvent.getChildWorkflowExecutionCompletedEventAttributes().getInitiatedEventId());
                map.put(dataField1, "result");
                map.put(dataValue1, historyEvent.getChildWorkflowExecutionCompletedEventAttributes().getResult());
                break;
            case ChildWorkflowExecutionCanceled:
                map.put(state, ERROR);
                ChildWorkflowExecutionCanceledEventAttributes childCanceled = historyEvent.getChildWorkflowExecutionCanceledEventAttributes();
                map.put(initialEventId, childCanceled.getInitiatedEventId());
                map.put(dataField1, "reason");
                map.put(dataValue1, childCanceled.getDetails());
                break;
            case ChildWorkflowExecutionFailed:
                map.put(state, ERROR);
                ChildWorkflowExecutionFailedEventAttributes childFailed = historyEvent.getChildWorkflowExecutionFailedEventAttributes();
                map.put(initialEventId, childFailed.getInitiatedEventId());
                map.put(dataField1, "reason");
                map.put(dataValue1, childFailed.getReason());
                map.put(dataField1, "details");
                map.put(dataValue2, childFailed.getDetails());
                break;
            case ChildWorkflowExecutionTerminated:
                map.put(state, ERROR);
                map.put(initialEventId, historyEvent.getChildWorkflowExecutionTerminatedEventAttributes().getInitiatedEventId());
                map.put(dataField1, "reason");
                map.put(dataValue1, "child workflow terminated");
                break;
            case ChildWorkflowExecutionTimedOut:
                map.put(state, ERROR);
                ChildWorkflowExecutionTimedOutEventAttributes childTimedOut = historyEvent.getChildWorkflowExecutionTimedOutEventAttributes();
                map.put(initialEventId, childTimedOut.getInitiatedEventId());
                map.put(dataField1, "timeoutType");
                map.put(dataValue1, childTimedOut.getTimeoutType());
                break;

            // Signal External Workflows
            case SignalExternalWorkflowExecutionInitiated:
                map.put(state, ACTIVE);
                map.put(initialEventId, historyEvent.getEventId());
                map.put(isInitialAction, true);
                SignalExternalWorkflowExecutionInitiatedEventAttributes signalInitiated = historyEvent.getSignalExternalWorkflowExecutionInitiatedEventAttributes();
                map.put(actionId, signalInitiated.getSignalName());
                map.put(dataField1, "input");
                map.put(dataValue1, signalInitiated.getInput());
                map.put(dataField2, "control");
                map.put(dataValue2, signalInitiated.getControl());
                break;
            case ExternalWorkflowExecutionSignaled:
                map.put(state, SUCCESS);
                ExternalWorkflowExecutionSignaledEventAttributes signalSignaled = historyEvent.getExternalWorkflowExecutionSignaledEventAttributes();
                map.put(initialEventId, signalSignaled.getInitiatedEventId());
                map.put(dataField1, "runId");
                map.put(dataValue1, signalSignaled.getWorkflowExecution().getRunId());
                break;

            // Signal Received (either from this workflow or external source)
            case WorkflowExecutionSignaled:
                map.put(state, SUCCESS);
                map.put(initialEventId, historyEvent.getEventId());
                WorkflowExecutionSignaledEventAttributes signaled = historyEvent.getWorkflowExecutionSignaledEventAttributes();
                map.put(actionId, signaled.getSignalName());
                map.put(dataField1, "input");
                map.put(dataValue1, signaled.getInput());
                break;

            // Markers
            case MarkerRecorded:
                map.put(state, SUCCESS);
                map.put(initialEventId, historyEvent.getEventId());
                map.put(isInitialAction, true);
                MarkerRecordedEventAttributes markerRecorded = historyEvent.getMarkerRecordedEventAttributes();
                map.put(actionId, markerRecorded.getMarkerName());
                map.put(dataField1, "details");
                map.put(dataValue1, markerRecorded.getDetails());
                break;

            // Decision Task Events
            case DecisionTaskCompleted:
                map.put(state, SUCCESS);
                map.put(initialEventId, historyEvent.getDecisionTaskCompletedEventAttributes().getScheduledEventId());
                map.put(dataField1, "context");
                map.put(dataValue1, historyEvent.getDecisionTaskCompletedEventAttributes().getExecutionContext());
                break;

            // WorkflowExecutionStarted
            case WorkflowExecutionStarted:
                map.put(state, SUCCESS);
                map.put(initialEventId, historyEvent.getEventId());
                map.put(dataField1, "input");
                map.put(dataValue1, historyEvent.getWorkflowExecutionStartedEventAttributes().getInput());
                break;

            // Workflow Error Events
            case WorkflowExecutionCancelRequested:
                map.put(state, CRITICAL);
                map.put(initialEventId, historyEvent.getEventId());
                map.put(dataField1, "cause");
                map.put(dataValue1, historyEvent.getWorkflowExecutionCancelRequestedEventAttributes().getCause());
                break;

            case ScheduleActivityTaskFailed:
                map.put(state, CRITICAL);
                map.put(initialEventId, historyEvent.getEventId());
                map.put(dataField1, "cause");
                map.put(dataValue1, historyEvent.getScheduleActivityTaskFailedEventAttributes().getCause());
                map.put(dataField1, "activityId");
                map.put(dataValue1, historyEvent.getScheduleActivityTaskFailedEventAttributes().getActivityId());
                break;

            case StartChildWorkflowExecutionFailed:
                map.put(state, CRITICAL);
                map.put(initialEventId, historyEvent.getStartChildWorkflowExecutionFailedEventAttributes().getInitiatedEventId());
                map.put(dataField1, "cause");
                map.put(dataValue1, historyEvent.getStartChildWorkflowExecutionFailedEventAttributes().getCause());
                map.put(dataField1, "control");
                map.put(dataValue1, historyEvent.getStartChildWorkflowExecutionFailedEventAttributes().getControl());
                break;

            case SignalExternalWorkflowExecutionFailed:
                map.put(state, CRITICAL);
                map.put(initialEventId, historyEvent.getSignalExternalWorkflowExecutionFailedEventAttributes().getInitiatedEventId());
                map.put(dataField1, "cause");
                map.put(dataValue1, historyEvent.getSignalExternalWorkflowExecutionFailedEventAttributes().getCause());
                map.put(dataField1, "control");
                map.put(dataValue1, historyEvent.getSignalExternalWorkflowExecutionFailedEventAttributes().getControl());
                break;

            // Informational EventTypes that are not useful for general decision making
            case DecisionTaskScheduled:
            case DecisionTaskStarted:
            case DecisionTaskTimedOut:
            case WorkflowExecutionCompleted:
            case CompleteWorkflowExecutionFailed:
            case WorkflowExecutionFailed:
            case FailWorkflowExecutionFailed:
            case WorkflowExecutionTimedOut:
            case WorkflowExecutionCanceled:
            case CancelWorkflowExecutionFailed:
            case WorkflowExecutionContinuedAsNew:
            case ContinueAsNewWorkflowExecutionFailed:
            case WorkflowExecutionTerminated:
            case ActivityTaskCancelRequested:
            case RequestCancelActivityTaskFailed:
            case RecordMarkerFailed:
            case CancelTimerFailed:
            case RequestCancelExternalWorkflowExecutionInitiated:
            case RequestCancelExternalWorkflowExecutionFailed:
            case ExternalWorkflowExecutionCancelRequested:
                map.put(state, INFO);
                map.put(initialEventId, historyEvent.getEventId());
                break;
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
        return this == o || o instanceof Event && getEventId().equals(((Event) o).getEventId());
    }

    /**
     * @return hashCode of this event's eventId.
     */
    public int hashCode() {
        return getEventId().hashCode();
    }

    @Override
    public String toString() {
        return fieldValueMap.toString();
    }

    /**
     * Sort by eventId descending (most recent event first).
     */
    public int compareTo(Event event) {
        return -getEventId().compareTo(event.getEventId());
    }
}
