package com.clario.swift;

import com.amazonaws.services.redshift.model.UnsupportedOptionException;
import com.amazonaws.services.simpleworkflow.model.EventType;
import com.amazonaws.services.simpleworkflow.model.HistoryEvent;

import java.util.Date;

import static com.amazonaws.services.simpleworkflow.model.EventType.*;
import static com.clario.swift.action.Action.State;
import static com.clario.swift.action.Action.State.*;

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
    private final Long initialEventId;
    private final boolean isInitialEvent;

    /**
     * Construct using an SWF <code>HistoryEvent</code>.
     * Use {@link ActionHistoryEvent#isActionHistoryEvent} to determine if a given <code>HistoryEvent</code> is allowed.
     *
     * @param historyEvent must be compatible with action event
     *
     * @see #isActionHistoryEvent(HistoryEvent)
     */
    public ActionHistoryEvent(HistoryEvent historyEvent) {
        if (isActionHistoryEvent(historyEvent)) {
            this.historyEvent = historyEvent;
            this.eventType = EventType.valueOf(historyEvent.getEventType());
            this.isInitialEvent = isInitialEventType(eventType);
            this.initialEventId = findInitialEventId(historyEvent);
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
            return findActionId(historyEvent);
        } else {
            throw new UnsupportedOperationException("Cannot get action id on non-initial action event: " + this);
        }
    }

    /**
     * @return {@link HistoryEvent#eventType} cast as {@link EventType} enumeration
     */
    public EventType getType() {
        return EventType.valueOf(historyEvent.getEventType());
    }

    /**
     * Find the {@link State} mapping for this history event.
     */
    public State getActionState() {
        return findActionState(historyEvent);
    }

    /**
     * @return {@link HistoryEvent#eventId}
     */
    public Long getEventId() {
        return historyEvent.getEventId();
    }

    /**
     * @return {@link HistoryEvent#eventTimestamp}
     */
    public Date getEventTimestamp() {
        return historyEvent.getEventTimestamp();
    }

    /**
     * Return the initial event id of the wrapped {@link HistoryEvent}.
     * <p/>
     * If this event is an initial event, return its event id
     * otherwise return it's pointer to the initial history event id
     */
    public Long getInitialEventId() {
        return initialEventId;
    }

    /**
     * @return result if this instance is an activity action completed event
     * @throws java.lang.UnsupportedOperationException if event type does not return a result
     */
    public String getResult() {
        String result = findResult(historyEvent);
        if (result == null) {
            throw new UnsupportedOptionException("Result not available for action: " + this);
        }
        return result;
    }

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
     * @return true if type indicates an event that initiated an action.
     */
    static boolean isInitialEventType(EventType eventType) {
        return ActivityTaskScheduled == eventType
            || TimerStarted == eventType
            || StartChildWorkflowExecutionInitiated == eventType
            || SignalExternalWorkflowExecutionInitiated == eventType;
    }

    /**
     * @return map an EventType to an action state.
     */
    static State findActionState(HistoryEvent historyEvent) {
        switch (EventType.valueOf(historyEvent.getEventType())) {
            // Activity Tasks
            case ActivityTaskScheduled:
            case ActivityTaskStarted:
                return active;
            case ActivityTaskCompleted:
                return success;
            case ActivityTaskCanceled:
            case ActivityTaskFailed:
            case ActivityTaskTimedOut:
                return error;

            // Timers
            case TimerStarted:
                return active;
            case TimerFired:
                // NOTE: This could be a 'retry' event but we don't have the control field handy yet.
                return success;
            case StartTimerFailed:
            case TimerCanceled:
                return error;

            // Child Workflows
            case StartChildWorkflowExecutionInitiated:
            case ChildWorkflowExecutionStarted:
                return active;
            case ChildWorkflowExecutionCompleted:
                return success;
            case ChildWorkflowExecutionCanceled:
            case ChildWorkflowExecutionFailed:
            case ChildWorkflowExecutionTerminated:
            case ChildWorkflowExecutionTimedOut:
                return error;

            // Signals
            case SignalExternalWorkflowExecutionInitiated:
                return active;
            case ExternalWorkflowExecutionSignaled:
                return success;
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
        switch (type) {
            case ActivityTaskCompleted:
                return historyEvent.getActivityTaskCompletedEventAttributes().getResult();
            case ChildWorkflowExecutionStarted:
                return historyEvent.getChildWorkflowExecutionStartedEventAttributes().getWorkflowExecution().getRunId();
            case ChildWorkflowExecutionCompleted:
                return historyEvent.getChildWorkflowExecutionCompletedEventAttributes().getResult();
        }
        return null;
    }

    private String findErrorReason(HistoryEvent historyEvent) {
        EventType type = EventType.valueOf(historyEvent.getEventType());
        switch (type) {
            case ActivityTaskCanceled:
                return historyEvent.getActivityTaskCanceledEventAttributes().getDetails();
            case ActivityTaskFailed:
                return historyEvent.getActivityTaskFailedEventAttributes().getReason() + " | " +
                    historyEvent.getActivityTaskFailedEventAttributes().getDetails();
            case ActivityTaskTimedOut:
                return historyEvent.getActivityTaskTimedOutEventAttributes().getTimeoutType() + " | " +
                    historyEvent.getActivityTaskTimedOutEventAttributes().getDetails();

            case StartTimerFailed:
                return historyEvent.getStartTimerFailedEventAttributes().getCause();
            case TimerCanceled:
                return "timer canceled";

            case ChildWorkflowExecutionCanceled:
                return historyEvent.getChildWorkflowExecutionCanceledEventAttributes().getDetails();
            case ChildWorkflowExecutionTerminated:
                return "child workflow terminated";
            case ChildWorkflowExecutionTimedOut:
                return historyEvent.getChildWorkflowExecutionTimedOutEventAttributes().getTimeoutType();
            case ChildWorkflowExecutionFailed:
                return historyEvent.getChildWorkflowExecutionFailedEventAttributes().getReason() + " | " +
                    historyEvent.getChildWorkflowExecutionFailedEventAttributes().getDetails();
        }
        return null;
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
        return SwiftUtil.DATE_TIME_FORMATTER.print(getEventTimestamp().getTime())
            + ' ' + getType()
            + ' ' + getEventId()
            + (isInitialEvent ? ' ' : " -> ")
            + (isInitialEvent ? getActionId() : initialEventId);
    }

    @Override
    public int compareTo(ActionHistoryEvent event) {
        return -getEventId().compareTo(event.getEventId());

    }
}
