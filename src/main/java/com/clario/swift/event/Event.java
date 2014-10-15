package com.clario.swift.event;

import com.amazonaws.services.simpleworkflow.model.EventType;
import com.amazonaws.services.simpleworkflow.model.HistoryEvent;
import org.joda.time.DateTime;

/**
 * @author George Coller
 */
public abstract class Event implements Comparable<Event> {

    protected final HistoryEvent historyEvent;

    protected Event(HistoryEvent historyEvent) {
        this.historyEvent = historyEvent;
    }

    public static Event create(HistoryEvent historyEvent) {
        return EventFactory.create(historyEvent);
    }

    public HistoryEvent getHistoryEvent() { return historyEvent; }

    public EventType getType() { return EventType.valueOf(historyEvent.getEventType()); }

    public Long getEventId() { return historyEvent.getEventId(); }

    public DateTime getEventTimestamp() { return new DateTime(historyEvent.getEventTimestamp()); }

    public abstract EventState getState();

    public abstract EventCategory getCategory();

    public abstract Long getInitialEventId();

    public abstract boolean isInitialAction();

    public abstract String getActionId();

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
     * StartTimerFailed                         | cause       | -
     * StartChildWorkflowExecutionInitiated     | input       | control
     * ChildWorkflowExecutionStarted            | WorkflowExecutionRunId | -
     * ChildWorkflowExecutionCompleted          | result      | -
     * ChildWorkflowExecutionCanceled           | details     | -
     * ChildWorkflowExecutionFailed             | reason      | details
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
    public abstract String getData1();

    public abstract String getData2();

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
        return String.format("%s: %s, %s, %s, %s", getType(), getEventId(), getInitialEventId(), getActionId(), getEventTimestamp());
    }

    /**
     * Sort by eventId descending (most recent event first).
     */
    public int compareTo(Event event) {
        return -getEventId().compareTo(event.getEventId());
    }
}
