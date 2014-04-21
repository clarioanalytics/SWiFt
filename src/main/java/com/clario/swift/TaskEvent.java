package com.clario.swift;

import com.amazonaws.services.redshift.model.UnsupportedOptionException;
import com.amazonaws.services.simpleworkflow.model.EventType;
import com.amazonaws.services.simpleworkflow.model.HistoryEvent;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.util.Date;

import static com.amazonaws.services.simpleworkflow.model.EventType.*;

/**
 * Class that unifies access to {@link HistoryEvent}s related to Activity, Timer, Child Workflow, or External Signal tasks.
 * <p/>
 * Basically trying to extract all the ugliness of Amazon's SWF model into one place so that this API can be cleaner.
 *
 * @author George Coller
 * @see HistoryInspector
 */
public class TaskEvent {
    private static final DateTimeFormatter DATE_TIME_FORMATTER = ISODateTimeFormat.basicDateTimeNoMillis();

    private final EventType eventType;
    private final boolean isInitialTaskEvent;
    private final long eventId;
    private final Date eventTimestamp;
    private final Long initialTaskEventId;
    private final String taskId;
    private final String result;

    /**
     * Construct using an SWF <code>HistoryEvent</code>.
     * Use {@link TaskEvent#isTaskEvent} to determine if a given <code>HistoryEvent</code> is allowed.
     *
     * @param historyEvent must be compatible with task event
     *
     * @see #isTaskEvent(HistoryEvent)
     */
    public TaskEvent(HistoryEvent historyEvent) {
        if (isTaskEvent(historyEvent)) {
            this.eventType = EventType.valueOf(historyEvent.getEventType());
            this.eventId = historyEvent.getEventId();
            this.eventTimestamp = historyEvent.getEventTimestamp();
            this.isInitialTaskEvent = isInitialEventType(eventType);
            this.initialTaskEventId = findInitialEventId(historyEvent);
            this.taskId = findTaskId(historyEvent);
            this.result = findResult(historyEvent);
        } else {
            throw new IllegalArgumentException("HistoryEvent type is not allowable: " + historyEvent);
        }
    }

    /**
     * Construct directly with values.
     * Unit test constructor
     */
    TaskEvent(Date eventTimestamp, long eventId, EventType eventType, boolean isInitialTaskEvent, Long initialTaskEventId, String taskId, String result) {
        this.eventType = eventType;
        this.isInitialTaskEvent = isInitialTaskEvent;
        this.eventId = eventId;
        this.eventTimestamp = eventTimestamp;
        this.initialTaskEventId = initialTaskEventId;
        this.taskId = taskId;
        this.result = result;
    }

    /**
     * Determine if a {@link HistoryEvent} has an SWF {@link EventType} that can be constructed as a <code>TaskEvent</code>.
     */
    public static boolean isTaskEvent(HistoryEvent historyEvent) {
        return findTaskState(EventType.valueOf(historyEvent.getEventType())) != null;
    }

    /**
     * Initial task events have an {@link #getType()} that starts an activity, timer, or child workflow decision.
     * Clients can use this to check if {@link #getEventId()} is available for this instance.
     *
     * @return true if task type is an initial task event
     * @see #getEventId()
     */
    public boolean isInitialTaskEvent() {
        return isInitialTaskEvent;
    }

    /**
     * @return unique task identifier for an initiator task event.
     * @throws UnsupportedOperationException if instance is not an initiator task event.
     * @see #isInitialTaskEvent()
     */
    public String getTaskId() {
        if (isInitialTaskEvent) {
            return taskId;
        } else {
            throw new UnsupportedOperationException("Cannot get task id on non-initial task event: " + this);
        }
    }

    /**
     * @return {@link HistoryEvent#eventType} cast as {@link EventType} enumeration
     */
    public EventType getType() {
        return eventType;
    }

    /**
     * @return proxy to {@link HistoryEvent#eventId}
     */
    public Long getEventId() {
        return eventId;
    }

    /**
     * @return proxy to {@link HistoryEvent#eventTimestamp}
     */
    public Date getEventTimestamp() {
        return eventTimestamp;
    }

    /**
     * Return the initial task event of the wrapped {@link HistoryEvent}.
     * <p/>
     * If this event is an initial event, return it's event id
     * otherwise return it's pointer to the initial event id
     */
    public Long getInitialTaskEventId() {
        return initialTaskEventId;
    }

    /**
     * @return result if this instance is an activity task completed event
     * @throws java.lang.UnsupportedOperationException if event type does not return a result
     */
    public String getResult() {
        if (ActivityTaskCompleted == getType()) {
            return result;
        }
        throw new UnsupportedOptionException("Result not available for task: " + this);
    }

    public TaskState getTaskState() {
        return findTaskState(eventType);
    }

    static boolean isInitialEventType(EventType eventType) {
        return ActivityTaskScheduled == eventType
            || TimerStarted == eventType
            || StartChildWorkflowExecutionInitiated == eventType
            || WorkflowExecutionSignaled == eventType;
    }

    static TaskState findTaskState(EventType eventType) {
        switch (eventType) {
            // Activity Tasks
            case ActivityTaskScheduled:
            case ActivityTaskStarted:
                return TaskState.decided;
            case ActivityTaskCompleted:
                return TaskState.finish_ok;
            case ActivityTaskCanceled:
            case ActivityTaskFailed:
            case ActivityTaskTimedOut:
                return TaskState.finish_error;

            // Timers
            case TimerStarted:
                return TaskState.decided;
            case TimerFired:
                return TaskState.finish_ok;
            case TimerCanceled:
                return TaskState.finish_error;

            // Child Workflows
            case StartChildWorkflowExecutionInitiated:
            case ChildWorkflowExecutionStarted:
                return TaskState.decided;
            case ChildWorkflowExecutionCompleted:
                return TaskState.finish_ok;
            case ChildWorkflowExecutionCanceled:
            case ChildWorkflowExecutionFailed:
            case ChildWorkflowExecutionTerminated:
            case ChildWorkflowExecutionTimedOut:
                return TaskState.finish_error;

            // Signals
            case WorkflowExecutionSignaled:
                return TaskState.finish_ok;
            default:
                return null;
        }
    }

    static Long findInitialEventId(HistoryEvent historyEvent) {
        switch (EventType.valueOf(historyEvent.getEventType())) {
            // Activity Tasks
            case ActivityTaskScheduled:
                return historyEvent.getEventId();
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
                return historyEvent.getEventId();
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
                return historyEvent.getEventId();
            default:
                return null;
        }
    }

    private static String findTaskId(HistoryEvent historyEvent) {
        switch (EventType.valueOf(historyEvent.getEventType())) {
            case ActivityTaskScheduled:
                return historyEvent.getActivityTaskScheduledEventAttributes().getActivityId();
            case TimerStarted:
                return historyEvent.getTimerStartedEventAttributes().getTimerId();
            case StartChildWorkflowExecutionInitiated:
                return historyEvent.getStartChildWorkflowExecutionInitiatedEventAttributes().getWorkflowId();
            case WorkflowExecutionSignaled:
                return String.valueOf(historyEvent.getWorkflowExecutionSignaledEventAttributes().getExternalInitiatedEventId());
            default:
                return null;
        }
    }

    private static String findResult(HistoryEvent historyEvent) {
        if (ActivityTaskCompleted == EventType.valueOf(historyEvent.getEventType())) {
            return historyEvent.getActivityTaskCompletedEventAttributes().getResult();
        }
        return null;
    }

    public boolean equals(Object o) {
        return this == o || o instanceof TaskEvent && eventId == ((TaskEvent) o).eventId;
    }

    public int hashCode() {
        return Long.valueOf(eventId).hashCode();
    }

    @Override
    public String toString() {
        return DATE_TIME_FORMATTER.print(eventTimestamp.getTime())
            + ' ' + eventType
            + ' ' + eventId
            + (isInitialTaskEvent ? ' ' : " -> ")
            + (isInitialTaskEvent ? taskId : initialTaskEventId);
    }
}
