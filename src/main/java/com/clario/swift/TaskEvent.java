package com.clario.swift;

import com.amazonaws.services.redshift.model.UnsupportedOptionException;
import com.amazonaws.services.simpleworkflow.model.EventType;
import com.amazonaws.services.simpleworkflow.model.HistoryEvent;

import java.util.Date;
import java.util.List;

import static com.amazonaws.services.simpleworkflow.model.EventType.*;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;

/**
 * Wraps a {@link HistoryEvent} related to Activity, Timer, Child Workflow, or External Signal started by a related {@link Task}.
 * <p/>
 * Allows more uniform handling of {@link HistoryEvent} objects.
 *
 * @author George Coller
 * @see com.clario.swift.HistoryInspector
 */
public class TaskEvent {
    public static final List<EventType> INITIATOR_EVENT_TYPES = unmodifiableList(asList(ActivityTaskScheduled, TimerStarted, StartChildWorkflowExecutionInitiated, WorkflowExecutionSignaled));
    public static final List<EventType> ACTIVITY_EVENT_TYPES = unmodifiableList(asList(ActivityTaskScheduled, ActivityTaskStarted, ActivityTaskCompleted, ActivityTaskFailed, ActivityTaskTimedOut, ActivityTaskCanceled));
    public static final List<EventType> TIMER_EVENT_TYPES = unmodifiableList(asList(TimerStarted, TimerFired, TimerCanceled));
    public static final List<EventType> CHILD_WORKFLOW_EVENT_TYPES = unmodifiableList(asList(StartChildWorkflowExecutionInitiated, ChildWorkflowExecutionStarted, ChildWorkflowExecutionCompleted, ChildWorkflowExecutionFailed, ChildWorkflowExecutionTimedOut, ChildWorkflowExecutionCanceled, ChildWorkflowExecutionTerminated));
    public static final List<EventType> SIGNAL_EVENT_TYPES = unmodifiableList(asList(WorkflowExecutionSignaled));

    private final HistoryEvent historyEvent;

    public TaskEvent(HistoryEvent historyEvent) {
        this.historyEvent = historyEvent;
    }

    /**
     * Determine if a {@link HistoryEvent} has an SWF {@link EventType} that this class cares about.
     */
    public static boolean isTaskEvent(HistoryEvent historyEvent) {
        EventType type = EventType.valueOf(historyEvent.getEventType());
        return ACTIVITY_EVENT_TYPES.contains(type) || TIMER_EVENT_TYPES.contains(type) || CHILD_WORKFLOW_EVENT_TYPES.contains(type) || SIGNAL_EVENT_TYPES.contains(type);
    }

    /**
     * Wrapped history event.
     *
     * @return wrapped history event.
     */
    public HistoryEvent getHistoryEvent() { return historyEvent; }

    /**
     * Initial task events have an {@link #getType()} that starts an activity, timer, or child workflow decision.
     * Clients can use this to check if {@link #getEventId()} is available for this instance.
     *
     * @return true if task type is an initial task event
     * @see #getEventId()
     */
    public boolean isInitialTaskEvent() {
        return INITIATOR_EVENT_TYPES.contains(getType());
    }

    /**
     * @return {@link HistoryEvent#eventType} cast as {@link EventType} enumeration
     */
    public EventType getType() {
        return EventType.valueOf(historyEvent.getEventType());
    }

    /**
     * @return proxy to {@link HistoryEvent#eventId}
     */
    public Long getEventId() {
        return historyEvent.getEventId();
    }

    /**
     * @return proxy to {@link HistoryEvent#eventTimestamp}
     */
    public Date getEventTimestamp() {
        return historyEvent.getEventTimestamp();
    }

    /**
     * Return the initial task event of the wrapped {@link HistoryEvent}.
     * <p/>
     * If this event is an initial event, return it's identifier converted to a Long.
     * otherwise return it's pointer to the initial event id (already a long).
     */
    public Long getInitialTaskEventId() {
        switch (getType()) {
            // Activity Tasks
            case ActivityTaskScheduled:
                return Long.valueOf(historyEvent.getActivityTaskScheduledEventAttributes().getActivityId());
            case ActivityTaskStarted:
                return historyEvent.getActivityTaskStartedEventAttributes().getScheduledEventId();
            case ActivityTaskCompleted:
                return historyEvent.getActivityTaskCompletedEventAttributes().getScheduledEventId();
            case ActivityTaskCanceled:
                return historyEvent.getActivityTaskCanceledEventAttributes().getScheduledEventId();
            case ActivityTaskFailed:
                return historyEvent.getActivityTaskFailedEventAttributes().getScheduledEventId();
            case ActivityTaskTimedOut:
                return historyEvent.getActivityTaskTimedOutEventAttributes().getScheduledEventId();
            // Timers
            case TimerStarted:
                return Long.valueOf(historyEvent.getTimerStartedEventAttributes().getTimerId());
            case TimerFired:
                return historyEvent.getTimerFiredEventAttributes().getStartedEventId();
            case TimerCanceled:
                return historyEvent.getTimerCanceledEventAttributes().getStartedEventId();
            // Child Workflows
            case StartChildWorkflowExecutionInitiated:
                return Long.valueOf(historyEvent.getStartChildWorkflowExecutionInitiatedEventAttributes().getWorkflowId());
            case ChildWorkflowExecutionCanceled:
                return historyEvent.getChildWorkflowExecutionCanceledEventAttributes().getInitiatedEventId();
            case ChildWorkflowExecutionCompleted:
                return historyEvent.getChildWorkflowExecutionCompletedEventAttributes().getInitiatedEventId();
            case ChildWorkflowExecutionFailed:
                return historyEvent.getChildWorkflowExecutionFailedEventAttributes().getInitiatedEventId();
            case ChildWorkflowExecutionStarted:
                return historyEvent.getChildWorkflowExecutionStartedEventAttributes().getInitiatedEventId();
            case ChildWorkflowExecutionTerminated:
                return historyEvent.getChildWorkflowExecutionTerminatedEventAttributes().getInitiatedEventId();
            case ChildWorkflowExecutionTimedOut:
                return historyEvent.getChildWorkflowExecutionTimedOutEventAttributes().getInitiatedEventId();
            // Signals
            case WorkflowExecutionSignaled:
                return historyEvent.getWorkflowExecutionSignaledEventAttributes().getExternalInitiatedEventId();
            default:
                throw new UnsupportedOperationException("Cannot get initiator event id for " + this);

        }
    }

    /**
     * @return unique identifier for an initiator <code>HistoryEvent</code>
     * @throws UnsupportedOperationException if instance is not an initiator.
     * @see #isInitialTaskEvent()
     */
    public String getId() {
        if (isInitialTaskEvent()) {
            return getInitialTaskEventId().toString();
        } else {
            throw new UnsupportedOperationException("Cannot get id on non-initial task " + this);
        }
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    public boolean equals(Object o) {
        return historyEvent.equals(o);
    }

    public int hashCode() {
        return historyEvent.hashCode();
    }

    public String toString() {
        return format("%s: %s", getClass().getSimpleName(), historyEvent);
    }

    /**
     * @return result if this instance is an activity task completed event
     * @throws java.lang.UnsupportedOperationException if event type does not return a result
     */
    public String getResult() {
        EventType type = getType();
        if (ActivityTaskCompleted == type) {
            return historyEvent.getActivityTaskCompletedEventAttributes().getResult();
        }
        throw new UnsupportedOptionException("Result not available for task with event type: " + type);
    }
}
