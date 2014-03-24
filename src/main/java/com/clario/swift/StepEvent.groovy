package com.clario.swift

import com.amazonaws.services.simpleworkflow.model.EventType
import com.amazonaws.services.simpleworkflow.model.HistoryEvent

import static com.amazonaws.services.simpleworkflow.model.EventType.*

/**
 * Wraps a {@link HistoryEvent} related to Activity, Timer, or Child Workflow processes started by a related {@link DecisionStep}.
 * Allows handling history events in a more uniform way.
 * @author George Coller
 * @see HistoryInspector
 */
public class StepEvent {

    public static final List<EventType> WORKFLOW_EVENT_TYPES = [EventType.WorkflowExecutionStarted]
    /**
     * Map of initiator {@link EventType} and initiator uniqueId method
     */
    public static final Map<EventType, Closure> INITIATOR_EVENT_TYPES = [
            (ActivityTaskScheduled): { HistoryEvent he -> he.activityTaskScheduledEventAttributes.activityId },
            (TimerStarted): { HistoryEvent he -> he.timerStartedEventAttributes.timerId },
            (StartChildWorkflowExecutionInitiated): { HistoryEvent he -> he.startChildWorkflowExecutionInitiatedEventAttributes.workflowId },
            (WorkflowExecutionSignaled): { HistoryEvent he -> he.workflowExecutionSignaledEventAttributes.signalName }
    ].asImmutable()
    public static final List<EventType> ACTIVITY_EVENT_TYPES =
            [ActivityTaskScheduled, ActivityTaskStarted, ActivityTaskCompleted, ActivityTaskFailed, ActivityTaskTimedOut, ActivityTaskCanceled].asImmutable()
    public static final List<EventType> TIMER_EVENT_TYPES =
            [TimerStarted, TimerFired, TimerCanceled].asImmutable()
    public static final List<EventType> CHILD_WORKFLOW_EVENT_TYPES =
            [StartChildWorkflowExecutionInitiated, ChildWorkflowExecutionStarted, ChildWorkflowExecutionCompleted, ChildWorkflowExecutionFailed, ChildWorkflowExecutionTimedOut, ChildWorkflowExecutionCanceled, ChildWorkflowExecutionTerminated].asImmutable()
    public static final List<EventType> SIGNAL_EVENT_TYPES =
            [WorkflowExecutionSignaled].asImmutable()


    public static final String ACTIVITY_EVENT_ID_METHOD = 'scheduledEventId'
    public static final String TIMER_EVENT_ID_METHOD = 'startedEventId'
    public static final String CHILD_WORKFLOW_EVENT_ID_METHOD = 'initiatedEventId'
    public static final String SIGNAL_EVENT_ID_METHOD = 'externalInitiatedEventId'

    private HistoryEvent historyEvent

    static boolean isStepEvent(HistoryEvent historyEvent) {
        EventType type = historyEvent.eventType as EventType
        return ACTIVITY_EVENT_TYPES.contains(type) || TIMER_EVENT_TYPES.contains(type) || CHILD_WORKFLOW_EVENT_TYPES.contains(type) || SIGNAL_EVENT_TYPES.contains(type)
    }

    StepEvent(HistoryEvent historyEvent) {
        this.historyEvent = historyEvent
    }

    /**
     * Wrapped history event.
     * @return wrapped history event.
     */
    HistoryEvent getHistoryEvent() {
        return historyEvent
    }

    /**
     * Initial step events have an {@link #getType()} that starts an activity, timer, or child workflow decision.
     * Clients can use this to check if {@link #getEventId()} is available for this instance.
     * @return true if step type is an initial step event
     * @see #getEventId()
     */
    boolean isInitialStepEvent() {
        return INITIATOR_EVENT_TYPES.containsKey(type)
    }

    /**
     * Return the specific <code> HistoryEvent.get<EventType>EventAttributes()<code> object related to this instance's {@link EventType}.
     * For example for {@link EventType#ActivityTaskCompleted} this method will return the object {@link HistoryEvent#getActivityTaskCanceledEventAttributes()}.
     * @return the event attributes
     * @see HistoryEvent
     */
    Object getAttributes() {
        // groovy reflection magics
        historyEvent["${eventType[0].toLowerCase()}${eventType.substring(1)}EventAttributes"]
    }

    /**
     * @return proxy to {@link HistoryEvent#eventType}
     * @see #getType()
     */
    String getEventType() {
        return historyEvent.eventType
    }

    /**
     * @return {@link HistoryEvent#eventType} cast as {@link EventType} enumeration
     * @see #getEventType()
     */
    EventType getType() {
        return historyEvent.eventType as EventType
    }

    /**
     * @return proxy to {@link HistoryEvent#eventId}
     */
    Long getEventId() {
        historyEvent.eventId
    }

    /**
     * @return proxy to {@link HistoryEvent#eventTimestamp}
     */
    Date getEventTimestamp() {
        historyEvent.eventTimestamp
    }

    /**
     * Retrieve the eventId of the initial step event related to this instance.
     * If this instance is the initial step, return this {@link HistoryEvent#eventId}.
     */
    Long getInitialStepEventId() {
        if (initialStepEvent) {
            return historyEvent.eventId
        } else {
            if (ACTIVITY_EVENT_TYPES.contains(type)) {
                attributes[ACTIVITY_EVENT_ID_METHOD] as Long
            } else if (TIMER_EVENT_TYPES.contains(type)) {
                attributes[TIMER_EVENT_ID_METHOD] as Long
            } else if (CHILD_WORKFLOW_EVENT_TYPES.contains(type)) {
                attributes[CHILD_WORKFLOW_EVENT_ID_METHOD] as Long
            } else if (SIGNAL_EVENT_TYPES.contains(type)) {
                attributes[SIGNAL_EVENT_ID_METHOD] as Long
            } else {
                throw new UnsupportedOperationException("Cannot get initiator event id for ${this}")
            }
        }
    }

    /**
     * @return unique identifier for an initiator <code>HistoryEvent</code>
     * @throws UnsupportedOperationException if instance is not an initiator.
     * @see #isInitialStepEvent()
     */
    String getStepId() {
        if (initialStepEvent) {
            INITIATOR_EVENT_TYPES[type](historyEvent)
        } else {
            throw new UnsupportedOperationException("Cannot get stepId on non-initial step $this")
        }
    }

    boolean equals(o) {
        if (this.is(o)) {
            return true
        }
        if (getClass() != o.class) {
            return false
        }

        StepEvent hpEvent = (StepEvent) o

        if (historyEvent != hpEvent.historyEvent) {
            return false
        }

        return true
    }

    int hashCode() {
        return historyEvent.hashCode()
    }


    String toString() {
        return "StepEvent:$historyEvent"
    }
}