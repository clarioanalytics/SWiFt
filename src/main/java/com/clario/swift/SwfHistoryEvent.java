package com.clario.swift;

import com.amazonaws.services.redshift.model.UnsupportedOptionException;
import com.amazonaws.services.simpleworkflow.model.EventType;
import com.amazonaws.services.simpleworkflow.model.HistoryEvent;
import com.clario.swift.action.SwfAction;

import java.util.Date;

import static com.amazonaws.services.simpleworkflow.model.EventType.*;
import static com.clario.swift.action.SwfAction.ActionState.*;

/**
 * Class that unifies access to {@link HistoryEvent}s related to Activity, Timer, Child Workflow, or External Signal activities.
 * <p/>
 * Basically trying to extract all the ugliness of Amazon's SWF model into one place so that this API can be cleaner.
 *
 * @author George Coller
 * @see SwfHistory
 */
public class SwfHistoryEvent {

    private final EventType eventType;
    private final boolean isInitialEvent;
    private final long eventId;
    private final Date eventTimestamp;
    private final Long initialEventId;
    private final String actionId;
    private final String result;

    /**
     * Construct using an SWF <code>HistoryEvent</code>.
     * Use {@link SwfHistoryEvent#isActionHistoryEvent} to determine if a given <code>HistoryEvent</code> is allowed.
     *
     * @param historyEvent must be compatible with action event
     *
     * @see #isActionHistoryEvent(HistoryEvent)
     */
    public SwfHistoryEvent(HistoryEvent historyEvent) {
        if (isActionHistoryEvent(historyEvent)) {
            this.eventType = EventType.valueOf(historyEvent.getEventType());
            this.eventId = historyEvent.getEventId();
            this.eventTimestamp = historyEvent.getEventTimestamp();
            this.isInitialEvent = isInitialEventType(eventType);
            this.initialEventId = findInitialEventId(historyEvent);
            this.actionId = findActionId(historyEvent);
            this.result = findResult(historyEvent);
        } else {
            throw new IllegalArgumentException("HistoryEvent type is not allowable: " + historyEvent);
        }
    }

    /**
     * Construct directly with values.
     * Unit test constructor
     */
    SwfHistoryEvent(Date eventTimestamp, long eventId, EventType eventType, boolean isInitialEvent, Long initialEventId, String actionId,
                    String result) {
        this.eventType = eventType;
        this.isInitialEvent = isInitialEvent;
        this.eventId = eventId;
        this.eventTimestamp = eventTimestamp;
        this.initialEventId = initialEventId;
        this.actionId = actionId;
        this.result = result;
    }

    /**
     * Determine if a {@link HistoryEvent} has an SWF {@link EventType} that can be constructed as a <code>ActionEvent</code>.
     */
    public static boolean isActionHistoryEvent(HistoryEvent historyEvent) {
        return findActionState(EventType.valueOf(historyEvent.getEventType())) != null;
    }

    /**
     * Initial action events have an {@link #getType()} that starts an activity, timer, or child workflow decision.
     * Clients can use this to check if {@link #getEventId()} is available for this instance.
     *
     * @return true if action type is an initial action event
     * @see #getEventId()
     */
    public boolean isInitialEvent() {
        return isInitialEvent;
    }

    /**
     * @return unique action identifier for an initiator action event.
     * @throws UnsupportedOperationException if instance is not an initiator action event.
     * @see #isInitialEvent()
     */
    public String getActionId() {
        if (isInitialEvent) {
            return actionId;
        } else {
            throw new UnsupportedOperationException("Cannot get action id on non-initial action event: " + this);
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
     * Return the initial action event of the wrapped {@link HistoryEvent}.
     * <p/>
     * If this event is an initial event, return it's event id
     * otherwise return it's pointer to the initial event id
     */
    public Long getInitialEventId() {
        return initialEventId;
    }

    /**
     * @return result if this instance is an activity action completed event
     * @throws java.lang.UnsupportedOperationException if event type does not return a result
     */
    public String getResult() {
        if (result == null) {
            throw new UnsupportedOptionException("Result not available for action: " + this);
        }
        return result;
    }

    public SwfAction.ActionState getActionState() {
        return findActionState(eventType);
    }

    static boolean isInitialEventType(EventType eventType) {
        return ActivityTaskScheduled == eventType
            || TimerStarted == eventType
            || StartChildWorkflowExecutionInitiated == eventType
            || SignalExternalWorkflowExecutionInitiated == eventType;
    }

    static SwfAction.ActionState findActionState(EventType eventType) {
        switch (eventType) {
            // Activity Tasks
            case ActivityTaskScheduled:
            case ActivityTaskStarted:
                return started;
            case ActivityTaskCompleted:
                return finish_ok;
            case ActivityTaskCanceled:
            case ActivityTaskFailed:
            case ActivityTaskTimedOut:
                return finish_error;

            // Timers
            case TimerStarted:
                return started;
            case TimerFired:
                return finish_ok;
            case TimerCanceled:
                return finish_error;

            // Child Workflows
            case StartChildWorkflowExecutionInitiated:
            case ChildWorkflowExecutionStarted:
                return started;
            case ChildWorkflowExecutionCompleted:
                return finish_ok;
            case ChildWorkflowExecutionCanceled:
            case ChildWorkflowExecutionFailed:
            case ChildWorkflowExecutionTerminated:
            case ChildWorkflowExecutionTimedOut:
                return finish_error;

            // Signals
            case SignalExternalWorkflowExecutionInitiated:
                return started;
            case ExternalWorkflowExecutionSignaled:
                return finish_ok;
            case SignalExternalWorkflowExecutionFailed:
                return finish_error;
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
                return historyEvent.getEventId();
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

            // Signal External Workflow
            case SignalExternalWorkflowExecutionInitiated:
                return historyEvent.getEventId();
            case ExternalWorkflowExecutionSignaled:
                return historyEvent.getExternalWorkflowExecutionSignaledEventAttributes().getInitiatedEventId();
            case SignalExternalWorkflowExecutionFailed:
                return historyEvent.getSignalExternalWorkflowExecutionFailedEventAttributes().getInitiatedEventId();
            default:
                return null;
        }
    }

    static String findActionId(HistoryEvent historyEvent) {
        switch (EventType.valueOf(historyEvent.getEventType())) {
            case ActivityTaskScheduled:
                return historyEvent.getActivityTaskScheduledEventAttributes().getActivityId();
            case TimerStarted:
                return historyEvent.getTimerStartedEventAttributes().getTimerId();
            case StartChildWorkflowExecutionInitiated:
                return historyEvent.getStartChildWorkflowExecutionInitiatedEventAttributes().getWorkflowId();
            case SignalExternalWorkflowExecutionInitiated:
                return historyEvent.getSignalExternalWorkflowExecutionInitiatedEventAttributes().getSignalName();
            default:
                return null;
        }
    }

    static String findResult(HistoryEvent historyEvent) {
        EventType type = EventType.valueOf(historyEvent.getEventType());
        if (ActivityTaskCompleted == type) {
            return historyEvent.getActivityTaskCompletedEventAttributes().getResult();
        } else if (ChildWorkflowExecutionStarted == type) {
            return historyEvent.getChildWorkflowExecutionStartedEventAttributes().getWorkflowExecution().getRunId();
        } else if (ChildWorkflowExecutionCompleted == type) {
            return historyEvent.getChildWorkflowExecutionCompletedEventAttributes().getResult();
        }
        return null;
    }

    public boolean equals(Object o) {
        return this == o || o instanceof SwfHistoryEvent && eventId == ((SwfHistoryEvent) o).eventId;
    }

    public int hashCode() {
        return Long.valueOf(eventId).hashCode();
    }

    @Override
    public String toString() {
        return SwiftUtil.DATE_TIME_FORMATTER.print(eventTimestamp.getTime())
            + ' ' + eventType
            + ' ' + eventId
            + (isInitialEvent ? ' ' : " -> ")
            + (isInitialEvent ? actionId : initialEventId);
    }

}
