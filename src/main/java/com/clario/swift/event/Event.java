package com.clario.swift.event;

import com.amazonaws.services.simpleworkflow.model.EventType;
import com.amazonaws.services.simpleworkflow.model.HistoryEvent;
import org.joda.time.DateTime;

import static com.amazonaws.services.simpleworkflow.model.EventType.*;
import static com.clario.swift.event.EventCategory.*;
import static com.clario.swift.event.EventState.*;
import static java.lang.String.format;


/**
 * Generated Event Class consolidates SWF {@link HistoryEvent} types
 * so groups of similar event types can be accessed in a uniform way.
 *
 * @author George Coller
 */
public class Event implements Comparable<Event> {

    private final HistoryEvent historyEvent;

    public Event(HistoryEvent historyEvent) { this.historyEvent = historyEvent; }

    public HistoryEvent getHistoryEvent() { return historyEvent; }

    public EventType getType() { return EventType.valueOf(historyEvent.getEventType()); }

    public Long getEventId() { return historyEvent.getEventId(); }

    public DateTime getEventTimestamp() { return new DateTime(historyEvent.getEventTimestamp()); }

    public EventCategory getCategory() {
        if (WorkflowExecutionStarted == getType()) { return WORKFLOW; }
        if (WorkflowExecutionCancelRequested == getType()) { return WORKFLOW; }
        if (WorkflowExecutionCompleted == getType()) { return WORKFLOW; }
        if (CompleteWorkflowExecutionFailed == getType()) { return WORKFLOW; }
        if (WorkflowExecutionFailed == getType()) { return WORKFLOW; }
        if (FailWorkflowExecutionFailed == getType()) { return WORKFLOW; }
        if (WorkflowExecutionTimedOut == getType()) { return WORKFLOW; }
        if (WorkflowExecutionCanceled == getType()) { return WORKFLOW; }
        if (CancelWorkflowExecutionFailed == getType()) { return WORKFLOW; }
        if (WorkflowExecutionContinuedAsNew == getType()) { return WORKFLOW; }
        if (ContinueAsNewWorkflowExecutionFailed == getType()) { return WORKFLOW; }
        if (WorkflowExecutionTerminated == getType()) { return WORKFLOW; }
        if (DecisionTaskScheduled == getType()) { return DECISION; }
        if (DecisionTaskStarted == getType()) { return DECISION; }
        if (DecisionTaskCompleted == getType()) { return DECISION; }
        if (DecisionTaskTimedOut == getType()) { return DECISION; }
        if (ActivityTaskScheduled == getType()) { return ACTION; }
        if (ScheduleActivityTaskFailed == getType()) { return ACTION; }
        if (ActivityTaskStarted == getType()) { return ACTION; }
        if (ActivityTaskCompleted == getType()) { return ACTION; }
        if (ActivityTaskFailed == getType()) { return ACTION; }
        if (ActivityTaskTimedOut == getType()) { return ACTION; }
        if (ActivityTaskCanceled == getType()) { return ACTION; }
        if (ActivityTaskCancelRequested == getType()) { return ACTION; }
        if (RequestCancelActivityTaskFailed == getType()) { return ACTION; }
        if (WorkflowExecutionSignaled == getType()) { return SIGNAL; }
        if (MarkerRecorded == getType()) { return ACTION; }
        if (RecordMarkerFailed == getType()) { return ACTION; }
        if (TimerStarted == getType()) { return ACTION; }
        if (StartTimerFailed == getType()) { return ACTION; }
        if (TimerFired == getType()) { return ACTION; }
        if (TimerCanceled == getType()) { return ACTION; }
        if (CancelTimerFailed == getType()) { return ACTION; }
        if (StartChildWorkflowExecutionInitiated == getType()) { return ACTION; }
        if (StartChildWorkflowExecutionFailed == getType()) { return ACTION; }
        if (ChildWorkflowExecutionStarted == getType()) { return ACTION; }
        if (ChildWorkflowExecutionCompleted == getType()) { return ACTION; }
        if (ChildWorkflowExecutionFailed == getType()) { return ACTION; }
        if (ChildWorkflowExecutionTimedOut == getType()) { return ACTION; }
        if (ChildWorkflowExecutionCanceled == getType()) { return ACTION; }
        if (ChildWorkflowExecutionTerminated == getType()) { return ACTION; }
        if (SignalExternalWorkflowExecutionInitiated == getType()) { return ACTION; }
        if (SignalExternalWorkflowExecutionFailed == getType()) { return ACTION; }
        if (ExternalWorkflowExecutionSignaled == getType()) { return ACTION; }
        if (RequestCancelExternalWorkflowExecutionInitiated == getType()) { return ACTION; }
        if (RequestCancelExternalWorkflowExecutionFailed == getType()) { return ACTION; }
        if (ExternalWorkflowExecutionCancelRequested == getType()) { return ACTION; }
        throw new IllegalArgumentException("Unknown EventType " + getType());
    }

    public EventState getState() {
        if (WorkflowExecutionStarted == getType()) { return ACTIVE; }
        if (WorkflowExecutionCancelRequested == getType()) { return ACTIVE; }
        if (WorkflowExecutionCompleted == getType()) { return SUCCESS; }
        if (CompleteWorkflowExecutionFailed == getType()) { return DIAGNOSTIC; }
        if (WorkflowExecutionFailed == getType()) { return ERROR; }
        if (FailWorkflowExecutionFailed == getType()) { return ERROR; }
        if (WorkflowExecutionTimedOut == getType()) { return ERROR; }
        if (WorkflowExecutionCanceled == getType()) { return ERROR; }
        if (CancelWorkflowExecutionFailed == getType()) { return DIAGNOSTIC; }
        if (WorkflowExecutionContinuedAsNew == getType()) { return ACTIVE; }
        if (ContinueAsNewWorkflowExecutionFailed == getType()) { return ERROR; }
        if (WorkflowExecutionTerminated == getType()) { return ERROR; }
        if (DecisionTaskScheduled == getType()) { return ACTIVE; }
        if (DecisionTaskStarted == getType()) { return ACTIVE; }
        if (DecisionTaskCompleted == getType()) { return SUCCESS; }
        if (DecisionTaskTimedOut == getType()) { return ERROR; }
        if (ActivityTaskScheduled == getType()) { return ACTIVE; }
        if (ScheduleActivityTaskFailed == getType()) { return ERROR; }
        if (ActivityTaskStarted == getType()) { return ACTIVE; }
        if (ActivityTaskCompleted == getType()) { return SUCCESS; }
        if (ActivityTaskFailed == getType()) { return ERROR; }
        if (ActivityTaskTimedOut == getType()) { return ERROR; }
        if (ActivityTaskCanceled == getType()) { return ERROR; }
        if (ActivityTaskCancelRequested == getType()) { return ERROR; }
        if (RequestCancelActivityTaskFailed == getType()) { return ERROR; }
        if (WorkflowExecutionSignaled == getType()) { return SUCCESS; }
        if (MarkerRecorded == getType()) { return SUCCESS; }
        if (RecordMarkerFailed == getType()) { return ERROR; }
        if (TimerStarted == getType()) { return ACTIVE; }
        if (StartTimerFailed == getType()) { return ERROR; }
        if (TimerFired == getType()) { return SUCCESS; }
        if (TimerCanceled == getType()) { return SUCCESS; }
        if (CancelTimerFailed == getType()) { return ACTIVE; }
        if (StartChildWorkflowExecutionInitiated == getType()) { return ACTIVE; }
        if (StartChildWorkflowExecutionFailed == getType()) { return ERROR; }
        if (ChildWorkflowExecutionStarted == getType()) { return ACTIVE; }
        if (ChildWorkflowExecutionCompleted == getType()) { return SUCCESS; }
        if (ChildWorkflowExecutionFailed == getType()) { return ERROR; }
        if (ChildWorkflowExecutionTimedOut == getType()) { return ERROR; }
        if (ChildWorkflowExecutionCanceled == getType()) { return ERROR; }
        if (ChildWorkflowExecutionTerminated == getType()) { return ERROR; }
        if (SignalExternalWorkflowExecutionInitiated == getType()) { return ACTIVE; }
        if (SignalExternalWorkflowExecutionFailed == getType()) { return ERROR; }
        if (ExternalWorkflowExecutionSignaled == getType()) { return SUCCESS; }
        if (RequestCancelExternalWorkflowExecutionInitiated == getType()) { return SUCCESS; }
        if (RequestCancelExternalWorkflowExecutionFailed == getType()) { return ERROR; }
        if (ExternalWorkflowExecutionCancelRequested == getType()) { return ACTIVE; }
        throw new IllegalArgumentException("Unknown EventType " + getType());
    }

    public Long getInitialEventId() {
        if (WorkflowExecutionStarted == getType()) { return historyEvent.getEventId(); }
        if (WorkflowExecutionCancelRequested == getType()) { return historyEvent.getEventId(); }
        if (WorkflowExecutionCompleted == getType()) { return historyEvent.getEventId(); }
        if (CompleteWorkflowExecutionFailed == getType()) { return historyEvent.getEventId(); }
        if (WorkflowExecutionFailed == getType()) { return historyEvent.getEventId(); }
        if (FailWorkflowExecutionFailed == getType()) { return historyEvent.getEventId(); }
        if (WorkflowExecutionTimedOut == getType()) { return historyEvent.getEventId(); }
        if (WorkflowExecutionCanceled == getType()) { return historyEvent.getEventId(); }
        if (CancelWorkflowExecutionFailed == getType()) { return historyEvent.getEventId(); }
        if (WorkflowExecutionContinuedAsNew == getType()) { return historyEvent.getEventId(); }
        if (ContinueAsNewWorkflowExecutionFailed == getType()) { return historyEvent.getEventId(); }
        if (WorkflowExecutionTerminated == getType()) { return historyEvent.getEventId(); }
        if (DecisionTaskScheduled == getType()) { return historyEvent.getEventId(); }
        if (DecisionTaskStarted == getType()) { return historyEvent.getEventId(); }
        if (DecisionTaskCompleted == getType()) { return historyEvent.getDecisionTaskCompletedEventAttributes().getScheduledEventId(); }
        if (DecisionTaskTimedOut == getType()) { return historyEvent.getEventId(); }
        if (ActivityTaskScheduled == getType()) { return historyEvent.getEventId(); }
        if (ScheduleActivityTaskFailed == getType()) { return historyEvent.getEventId(); }
        if (ActivityTaskStarted == getType()) { return historyEvent.getActivityTaskStartedEventAttributes().getScheduledEventId(); }
        if (ActivityTaskCompleted == getType()) { return historyEvent.getActivityTaskCompletedEventAttributes().getScheduledEventId(); }
        if (ActivityTaskFailed == getType()) { return historyEvent.getActivityTaskFailedEventAttributes().getScheduledEventId(); }
        if (ActivityTaskTimedOut == getType()) { return historyEvent.getActivityTaskTimedOutEventAttributes().getScheduledEventId(); }
        if (ActivityTaskCanceled == getType()) { return historyEvent.getActivityTaskCanceledEventAttributes().getScheduledEventId(); }
        if (ActivityTaskCancelRequested == getType()) { return historyEvent.getEventId(); }
        if (RequestCancelActivityTaskFailed == getType()) { return historyEvent.getEventId(); }
        if (WorkflowExecutionSignaled == getType()) { return historyEvent.getEventId(); }
        if (MarkerRecorded == getType()) { return historyEvent.getEventId(); }
        if (RecordMarkerFailed == getType()) { return historyEvent.getEventId(); }
        if (TimerStarted == getType()) { return historyEvent.getEventId(); }
        if (StartTimerFailed == getType()) { return null; }
        if (TimerFired == getType()) { return historyEvent.getTimerFiredEventAttributes().getStartedEventId(); }
        if (TimerCanceled == getType()) { return historyEvent.getTimerCanceledEventAttributes().getStartedEventId(); }
        if (CancelTimerFailed == getType()) { return historyEvent.getEventId(); }
        if (StartChildWorkflowExecutionInitiated == getType()) { return historyEvent.getEventId(); }
        if (StartChildWorkflowExecutionFailed == getType()) { return historyEvent.getStartChildWorkflowExecutionFailedEventAttributes().getInitiatedEventId(); }
        if (ChildWorkflowExecutionStarted == getType()) { return historyEvent.getChildWorkflowExecutionStartedEventAttributes().getInitiatedEventId(); }
        if (ChildWorkflowExecutionCompleted == getType()) { return historyEvent.getChildWorkflowExecutionCompletedEventAttributes().getInitiatedEventId(); }
        if (ChildWorkflowExecutionFailed == getType()) { return historyEvent.getChildWorkflowExecutionFailedEventAttributes().getInitiatedEventId(); }
        if (ChildWorkflowExecutionTimedOut == getType()) { return historyEvent.getChildWorkflowExecutionTimedOutEventAttributes().getInitiatedEventId(); }
        if (ChildWorkflowExecutionCanceled == getType()) { return historyEvent.getChildWorkflowExecutionCanceledEventAttributes().getInitiatedEventId(); }
        if (ChildWorkflowExecutionTerminated == getType()) { return historyEvent.getChildWorkflowExecutionTerminatedEventAttributes().getInitiatedEventId(); }
        if (SignalExternalWorkflowExecutionInitiated == getType()) { return historyEvent.getEventId(); }
        if (SignalExternalWorkflowExecutionFailed == getType()) { return historyEvent.getSignalExternalWorkflowExecutionFailedEventAttributes().getInitiatedEventId(); }
        if (ExternalWorkflowExecutionSignaled == getType()) { return historyEvent.getExternalWorkflowExecutionSignaledEventAttributes().getInitiatedEventId(); }
        if (RequestCancelExternalWorkflowExecutionInitiated == getType()) { return historyEvent.getEventId(); }
        if (RequestCancelExternalWorkflowExecutionFailed == getType()) { return historyEvent.getEventId(); }
        if (ExternalWorkflowExecutionCancelRequested == getType()) { return historyEvent.getEventId(); }
        throw new IllegalArgumentException("Unknown EventType " + getType());
    }

    public boolean isInitialAction() {
        if (WorkflowExecutionStarted == getType()) { return false; }
        if (WorkflowExecutionCancelRequested == getType()) { return false; }
        if (WorkflowExecutionCompleted == getType()) { return false; }
        if (CompleteWorkflowExecutionFailed == getType()) { return false; }
        if (WorkflowExecutionFailed == getType()) { return false; }
        if (FailWorkflowExecutionFailed == getType()) { return false; }
        if (WorkflowExecutionTimedOut == getType()) { return false; }
        if (WorkflowExecutionCanceled == getType()) { return false; }
        if (CancelWorkflowExecutionFailed == getType()) { return false; }
        if (WorkflowExecutionContinuedAsNew == getType()) { return false; }
        if (ContinueAsNewWorkflowExecutionFailed == getType()) { return false; }
        if (WorkflowExecutionTerminated == getType()) { return false; }
        if (DecisionTaskScheduled == getType()) { return false; }
        if (DecisionTaskStarted == getType()) { return false; }
        if (DecisionTaskCompleted == getType()) { return false; }
        if (DecisionTaskTimedOut == getType()) { return false; }
        if (ActivityTaskScheduled == getType()) { return true; }
        if (ScheduleActivityTaskFailed == getType()) { return false; }
        if (ActivityTaskStarted == getType()) { return false; }
        if (ActivityTaskCompleted == getType()) { return false; }
        if (ActivityTaskFailed == getType()) { return false; }
        if (ActivityTaskTimedOut == getType()) { return false; }
        if (ActivityTaskCanceled == getType()) { return false; }
        if (ActivityTaskCancelRequested == getType()) { return false; }
        if (RequestCancelActivityTaskFailed == getType()) { return false; }
        if (WorkflowExecutionSignaled == getType()) { return false; }
        if (MarkerRecorded == getType()) { return true; }
        if (RecordMarkerFailed == getType()) { return false; }
        if (TimerStarted == getType()) { return true; }
        if (StartTimerFailed == getType()) { return false; }
        if (TimerFired == getType()) { return false; }
        if (TimerCanceled == getType()) { return false; }
        if (CancelTimerFailed == getType()) { return false; }
        if (StartChildWorkflowExecutionInitiated == getType()) { return true; }
        if (StartChildWorkflowExecutionFailed == getType()) { return false; }
        if (ChildWorkflowExecutionStarted == getType()) { return false; }
        if (ChildWorkflowExecutionCompleted == getType()) { return false; }
        if (ChildWorkflowExecutionFailed == getType()) { return false; }
        if (ChildWorkflowExecutionTimedOut == getType()) { return false; }
        if (ChildWorkflowExecutionCanceled == getType()) { return false; }
        if (ChildWorkflowExecutionTerminated == getType()) { return false; }
        if (SignalExternalWorkflowExecutionInitiated == getType()) { return true; }
        if (SignalExternalWorkflowExecutionFailed == getType()) { return false; }
        if (ExternalWorkflowExecutionSignaled == getType()) { return false; }
        if (RequestCancelExternalWorkflowExecutionInitiated == getType()) { return false; }
        if (RequestCancelExternalWorkflowExecutionFailed == getType()) { return false; }
        if (ExternalWorkflowExecutionCancelRequested == getType()) { return true; }
        throw new IllegalArgumentException("Unknown EventType " + getType());
    }

    public String getActionId() {
        if (WorkflowExecutionStarted == getType()) { return null; }
        if (WorkflowExecutionCancelRequested == getType()) { return null; }
        if (WorkflowExecutionCompleted == getType()) { return null; }
        if (CompleteWorkflowExecutionFailed == getType()) { return null; }
        if (WorkflowExecutionFailed == getType()) { return null; }
        if (FailWorkflowExecutionFailed == getType()) { return null; }
        if (WorkflowExecutionTimedOut == getType()) { return null; }
        if (WorkflowExecutionCanceled == getType()) { return null; }
        if (CancelWorkflowExecutionFailed == getType()) { return null; }
        if (WorkflowExecutionContinuedAsNew == getType()) { return null; }
        if (ContinueAsNewWorkflowExecutionFailed == getType()) { return null; }
        if (WorkflowExecutionTerminated == getType()) { return null; }
        if (DecisionTaskScheduled == getType()) { return null; }
        if (DecisionTaskStarted == getType()) { return null; }
        if (DecisionTaskCompleted == getType()) { return null; }
        if (DecisionTaskTimedOut == getType()) { return null; }
        if (ActivityTaskScheduled == getType()) { return historyEvent.getActivityTaskScheduledEventAttributes().getActivityId(); }
        if (ScheduleActivityTaskFailed == getType()) { return null; }
        if (ActivityTaskStarted == getType()) { return null; }
        if (ActivityTaskCompleted == getType()) { return null; }
        if (ActivityTaskFailed == getType()) { return null; }
        if (ActivityTaskTimedOut == getType()) { return null; }
        if (ActivityTaskCanceled == getType()) { return null; }
        if (ActivityTaskCancelRequested == getType()) { return null; }
        if (RequestCancelActivityTaskFailed == getType()) { return null; }
        if (WorkflowExecutionSignaled == getType()) { return historyEvent.getWorkflowExecutionSignaledEventAttributes().getSignalName(); }
        if (MarkerRecorded == getType()) { return historyEvent.getMarkerRecordedEventAttributes().getMarkerName(); }
        if (RecordMarkerFailed == getType()) { return null; }
        if (TimerStarted == getType()) { return historyEvent.getTimerStartedEventAttributes().getTimerId(); }
        if (StartTimerFailed == getType()) { return historyEvent.getStartTimerFailedEventAttributes().getTimerId(); }
        if (TimerFired == getType()) { return historyEvent.getTimerFiredEventAttributes().getTimerId(); }
        if (TimerCanceled == getType()) { return historyEvent.getTimerCanceledEventAttributes().getTimerId(); }
        if (CancelTimerFailed == getType()) { return historyEvent.getCancelTimerFailedEventAttributes().getTimerId(); }
        if (StartChildWorkflowExecutionInitiated == getType()) { return historyEvent.getStartChildWorkflowExecutionInitiatedEventAttributes().getWorkflowId(); }
        if (StartChildWorkflowExecutionFailed == getType()) { return null; }
        if (ChildWorkflowExecutionStarted == getType()) { return null; }
        if (ChildWorkflowExecutionCompleted == getType()) { return null; }
        if (ChildWorkflowExecutionFailed == getType()) { return null; }
        if (ChildWorkflowExecutionTimedOut == getType()) { return null; }
        if (ChildWorkflowExecutionCanceled == getType()) { return null; }
        if (ChildWorkflowExecutionTerminated == getType()) { return null; }
        if (SignalExternalWorkflowExecutionInitiated == getType()) { return historyEvent.getSignalExternalWorkflowExecutionInitiatedEventAttributes().getSignalName(); }
        if (SignalExternalWorkflowExecutionFailed == getType()) { return null; }
        if (ExternalWorkflowExecutionSignaled == getType()) { return null; }
        if (RequestCancelExternalWorkflowExecutionInitiated == getType()) { return historyEvent.getRequestCancelExternalWorkflowExecutionInitiatedEventAttributes().getControl(); }
        if (RequestCancelExternalWorkflowExecutionFailed == getType()) { return historyEvent.getRequestCancelExternalWorkflowExecutionFailedEventAttributes().getControl(); }
        if (ExternalWorkflowExecutionCancelRequested == getType()) { return null; }
        throw new IllegalArgumentException("Unknown EventType " + getType());
    }

    public String getInput() {
        if (WorkflowExecutionStarted == getType()) { return historyEvent.getWorkflowExecutionStartedEventAttributes().getInput(); }
        if (WorkflowExecutionCancelRequested == getType()) { return null; }
        if (WorkflowExecutionCompleted == getType()) { return null; }
        if (CompleteWorkflowExecutionFailed == getType()) { return null; }
        if (WorkflowExecutionFailed == getType()) { return null; }
        if (FailWorkflowExecutionFailed == getType()) { return null; }
        if (WorkflowExecutionTimedOut == getType()) { return null; }
        if (WorkflowExecutionCanceled == getType()) { return null; }
        if (CancelWorkflowExecutionFailed == getType()) { return null; }
        if (WorkflowExecutionContinuedAsNew == getType()) { return historyEvent.getWorkflowExecutionContinuedAsNewEventAttributes().getInput(); }
        if (ContinueAsNewWorkflowExecutionFailed == getType()) { return null; }
        if (WorkflowExecutionTerminated == getType()) { return null; }
        if (DecisionTaskScheduled == getType()) { return null; }
        if (DecisionTaskStarted == getType()) { return null; }
        if (DecisionTaskCompleted == getType()) { return null; }
        if (DecisionTaskTimedOut == getType()) { return null; }
        if (ActivityTaskScheduled == getType()) { return historyEvent.getActivityTaskScheduledEventAttributes().getInput(); }
        if (ScheduleActivityTaskFailed == getType()) { return null; }
        if (ActivityTaskStarted == getType()) { return null; }
        if (ActivityTaskCompleted == getType()) { return null; }
        if (ActivityTaskFailed == getType()) { return null; }
        if (ActivityTaskTimedOut == getType()) { return null; }
        if (ActivityTaskCanceled == getType()) { return null; }
        if (ActivityTaskCancelRequested == getType()) { return null; }
        if (RequestCancelActivityTaskFailed == getType()) { return null; }
        if (WorkflowExecutionSignaled == getType()) { return historyEvent.getWorkflowExecutionSignaledEventAttributes().getInput(); }
        if (MarkerRecorded == getType()) { return null; }
        if (RecordMarkerFailed == getType()) { return null; }
        if (TimerStarted == getType()) { return "Timer Started"; }
        if (StartTimerFailed == getType()) { return null; }
        if (TimerFired == getType()) { return null; }
        if (TimerCanceled == getType()) { return null; }
        if (CancelTimerFailed == getType()) { return null; }
        if (StartChildWorkflowExecutionInitiated == getType()) { return historyEvent.getStartChildWorkflowExecutionInitiatedEventAttributes().getInput(); }
        if (StartChildWorkflowExecutionFailed == getType()) { return null; }
        if (ChildWorkflowExecutionStarted == getType()) { return null; }
        if (ChildWorkflowExecutionCompleted == getType()) { return null; }
        if (ChildWorkflowExecutionFailed == getType()) { return null; }
        if (ChildWorkflowExecutionTimedOut == getType()) { return null; }
        if (ChildWorkflowExecutionCanceled == getType()) { return null; }
        if (ChildWorkflowExecutionTerminated == getType()) { return null; }
        if (SignalExternalWorkflowExecutionInitiated == getType()) { return historyEvent.getSignalExternalWorkflowExecutionInitiatedEventAttributes().getInput(); }
        if (SignalExternalWorkflowExecutionFailed == getType()) { return null; }
        if (ExternalWorkflowExecutionSignaled == getType()) { return null; }
        if (RequestCancelExternalWorkflowExecutionInitiated == getType()) { return null; }
        if (RequestCancelExternalWorkflowExecutionFailed == getType()) { return null; }
        if (ExternalWorkflowExecutionCancelRequested == getType()) { return null; }
        throw new IllegalArgumentException("Unknown EventType " + getType());
    }

    public String getControl() {
        if (WorkflowExecutionStarted == getType()) { return null; }
        if (WorkflowExecutionCancelRequested == getType()) { return null; }
        if (WorkflowExecutionCompleted == getType()) { return null; }
        if (CompleteWorkflowExecutionFailed == getType()) { return null; }
        if (WorkflowExecutionFailed == getType()) { return null; }
        if (FailWorkflowExecutionFailed == getType()) { return null; }
        if (WorkflowExecutionTimedOut == getType()) { return null; }
        if (WorkflowExecutionCanceled == getType()) { return null; }
        if (CancelWorkflowExecutionFailed == getType()) { return null; }
        if (WorkflowExecutionContinuedAsNew == getType()) { return null; }
        if (ContinueAsNewWorkflowExecutionFailed == getType()) { return null; }
        if (WorkflowExecutionTerminated == getType()) { return null; }
        if (DecisionTaskScheduled == getType()) { return null; }
        if (DecisionTaskStarted == getType()) { return null; }
        if (DecisionTaskCompleted == getType()) { return null; }
        if (DecisionTaskTimedOut == getType()) { return null; }
        if (ActivityTaskScheduled == getType()) { return historyEvent.getActivityTaskScheduledEventAttributes().getControl(); }
        if (ScheduleActivityTaskFailed == getType()) { return null; }
        if (ActivityTaskStarted == getType()) { return null; }
        if (ActivityTaskCompleted == getType()) { return null; }
        if (ActivityTaskFailed == getType()) { return null; }
        if (ActivityTaskTimedOut == getType()) { return null; }
        if (ActivityTaskCanceled == getType()) { return null; }
        if (ActivityTaskCancelRequested == getType()) { return null; }
        if (RequestCancelActivityTaskFailed == getType()) { return null; }
        if (WorkflowExecutionSignaled == getType()) { return null; }
        if (MarkerRecorded == getType()) { return null; }
        if (RecordMarkerFailed == getType()) { return null; }
        if (TimerStarted == getType()) { return historyEvent.getTimerStartedEventAttributes().getControl(); }
        if (StartTimerFailed == getType()) { return null; }
        if (TimerFired == getType()) { return null; }
        if (TimerCanceled == getType()) { return null; }
        if (CancelTimerFailed == getType()) { return null; }
        if (StartChildWorkflowExecutionInitiated == getType()) { return historyEvent.getStartChildWorkflowExecutionInitiatedEventAttributes().getControl(); }
        if (StartChildWorkflowExecutionFailed == getType()) { return historyEvent.getStartChildWorkflowExecutionFailedEventAttributes().getControl(); }
        if (ChildWorkflowExecutionStarted == getType()) { return null; }
        if (ChildWorkflowExecutionCompleted == getType()) { return null; }
        if (ChildWorkflowExecutionFailed == getType()) { return null; }
        if (ChildWorkflowExecutionTimedOut == getType()) { return null; }
        if (ChildWorkflowExecutionCanceled == getType()) { return null; }
        if (ChildWorkflowExecutionTerminated == getType()) { return null; }
        if (SignalExternalWorkflowExecutionInitiated == getType()) { return historyEvent.getSignalExternalWorkflowExecutionInitiatedEventAttributes().getControl(); }
        if (SignalExternalWorkflowExecutionFailed == getType()) { return historyEvent.getSignalExternalWorkflowExecutionFailedEventAttributes().getControl(); }
        if (ExternalWorkflowExecutionSignaled == getType()) { return null; }
        if (RequestCancelExternalWorkflowExecutionInitiated == getType()) { return historyEvent.getRequestCancelExternalWorkflowExecutionInitiatedEventAttributes().getControl(); }
        if (RequestCancelExternalWorkflowExecutionFailed == getType()) { return historyEvent.getRequestCancelExternalWorkflowExecutionFailedEventAttributes().getControl(); }
        if (ExternalWorkflowExecutionCancelRequested == getType()) { return null; }
        throw new IllegalArgumentException("Unknown EventType " + getType());
    }

    public String getOutput() {
        if (WorkflowExecutionStarted == getType()) { return null; }
        if (WorkflowExecutionCancelRequested == getType()) { return null; }
        if (WorkflowExecutionCompleted == getType()) { return historyEvent.getWorkflowExecutionCompletedEventAttributes().getResult(); }
        if (CompleteWorkflowExecutionFailed == getType()) { return null; }
        if (WorkflowExecutionFailed == getType()) { return null; }
        if (FailWorkflowExecutionFailed == getType()) { return null; }
        if (WorkflowExecutionTimedOut == getType()) { return null; }
        if (WorkflowExecutionCanceled == getType()) { return null; }
        if (CancelWorkflowExecutionFailed == getType()) { return null; }
        if (WorkflowExecutionContinuedAsNew == getType()) { return null; }
        if (ContinueAsNewWorkflowExecutionFailed == getType()) { return null; }
        if (WorkflowExecutionTerminated == getType()) { return null; }
        if (DecisionTaskScheduled == getType()) { return null; }
        if (DecisionTaskStarted == getType()) { return null; }
        if (DecisionTaskCompleted == getType()) { return historyEvent.getDecisionTaskCompletedEventAttributes().getExecutionContext(); }
        if (DecisionTaskTimedOut == getType()) { return null; }
        if (ActivityTaskScheduled == getType()) { return null; }
        if (ScheduleActivityTaskFailed == getType()) { return null; }
        if (ActivityTaskStarted == getType()) { return null; }
        if (ActivityTaskCompleted == getType()) { return historyEvent.getActivityTaskCompletedEventAttributes().getResult(); }
        if (ActivityTaskFailed == getType()) { return null; }
        if (ActivityTaskTimedOut == getType()) { return null; }
        if (ActivityTaskCanceled == getType()) { return null; }
        if (ActivityTaskCancelRequested == getType()) { return null; }
        if (RequestCancelActivityTaskFailed == getType()) { return null; }
        if (WorkflowExecutionSignaled == getType()) { return historyEvent.getWorkflowExecutionSignaledEventAttributes().getInput(); }
        if (MarkerRecorded == getType()) { return historyEvent.getMarkerRecordedEventAttributes().getDetails(); }
        if (RecordMarkerFailed == getType()) { return null; }
        if (TimerStarted == getType()) { return null; }
        if (StartTimerFailed == getType()) { return null; }
        if (TimerFired == getType()) { return "Timer Fired"; }
        if (TimerCanceled == getType()) { return "Timer Canceled"; }
        if (CancelTimerFailed == getType()) { return null; }
        if (StartChildWorkflowExecutionInitiated == getType()) { return null; }
        if (StartChildWorkflowExecutionFailed == getType()) { return null; }
        if (ChildWorkflowExecutionStarted == getType()) { return null; }
        if (ChildWorkflowExecutionCompleted == getType()) { return historyEvent.getChildWorkflowExecutionCompletedEventAttributes().getResult(); }
        if (ChildWorkflowExecutionFailed == getType()) { return null; }
        if (ChildWorkflowExecutionTimedOut == getType()) { return null; }
        if (ChildWorkflowExecutionCanceled == getType()) { return null; }
        if (ChildWorkflowExecutionTerminated == getType()) { return null; }
        if (SignalExternalWorkflowExecutionInitiated == getType()) { return null; }
        if (SignalExternalWorkflowExecutionFailed == getType()) { return null; }
        if (ExternalWorkflowExecutionSignaled == getType()) { return historyEvent.getExternalWorkflowExecutionSignaledEventAttributes().getWorkflowExecution().getRunId(); }
        if (RequestCancelExternalWorkflowExecutionInitiated == getType()) { return null; }
        if (RequestCancelExternalWorkflowExecutionFailed == getType()) { return null; }
        if (ExternalWorkflowExecutionCancelRequested == getType()) { return null; }
        throw new IllegalArgumentException("Unknown EventType " + getType());
    }

    public String getReason() {
        if (WorkflowExecutionStarted == getType()) { return null; }
        if (WorkflowExecutionCancelRequested == getType()) { return "Workflow Execution Cancel Requested"; }
        if (WorkflowExecutionCompleted == getType()) { return null; }
        if (CompleteWorkflowExecutionFailed == getType()) { return "Complete Workflow Execution Failed"; }
        if (WorkflowExecutionFailed == getType()) { return "Workflow Execution Failed"; }
        if (FailWorkflowExecutionFailed == getType()) { return "Fail Workflow Execution Failed"; }
        if (WorkflowExecutionTimedOut == getType()) { return "Workflow Execution Timed Out"; }
        if (WorkflowExecutionCanceled == getType()) { return "Workflow Execution Canceled"; }
        if (CancelWorkflowExecutionFailed == getType()) { return "Cancel Workflow Execution Failed"; }
        if (WorkflowExecutionContinuedAsNew == getType()) { return null; }
        if (ContinueAsNewWorkflowExecutionFailed == getType()) { return "Continue As New Workflow Execution Failed"; }
        if (WorkflowExecutionTerminated == getType()) { return "Workflow Execution Terminated"; }
        if (DecisionTaskScheduled == getType()) { return null; }
        if (DecisionTaskStarted == getType()) { return null; }
        if (DecisionTaskCompleted == getType()) { return null; }
        if (DecisionTaskTimedOut == getType()) { return null; }
        if (ActivityTaskScheduled == getType()) { return null; }
        if (ScheduleActivityTaskFailed == getType()) { return "Schedule Activity Task Failed"; }
        if (ActivityTaskStarted == getType()) { return null; }
        if (ActivityTaskCompleted == getType()) { return null; }
        if (ActivityTaskFailed == getType()) { return historyEvent.getActivityTaskFailedEventAttributes().getReason(); }
        if (ActivityTaskTimedOut == getType()) { return historyEvent.getActivityTaskTimedOutEventAttributes().getTimeoutType(); }
        if (ActivityTaskCanceled == getType()) { return "Activity Task Canceled"; }
        if (ActivityTaskCancelRequested == getType()) { return "Activity Task Cancel Requested"; }
        if (RequestCancelActivityTaskFailed == getType()) { return "Request Cancel Activity Task Failed"; }
        if (WorkflowExecutionSignaled == getType()) { return null; }
        if (MarkerRecorded == getType()) { return null; }
        if (RecordMarkerFailed == getType()) { return "Record Marker Failed"; }
        if (TimerStarted == getType()) { return null; }
        if (StartTimerFailed == getType()) { return "Start Timer Failed"; }
        if (TimerFired == getType()) { return null; }
        if (TimerCanceled == getType()) { return null; }
        if (CancelTimerFailed == getType()) { return null; }
        if (StartChildWorkflowExecutionInitiated == getType()) { return null; }
        if (StartChildWorkflowExecutionFailed == getType()) { return "Start Child Workflow Execution Failed"; }
        if (ChildWorkflowExecutionStarted == getType()) { return null; }
        if (ChildWorkflowExecutionCompleted == getType()) { return null; }
        if (ChildWorkflowExecutionFailed == getType()) { return historyEvent.getChildWorkflowExecutionFailedEventAttributes().getReason(); }
        if (ChildWorkflowExecutionTimedOut == getType()) { return "Child Workflow Execution Timed Out"; }
        if (ChildWorkflowExecutionCanceled == getType()) { return "Child Workflow Execution Canceled"; }
        if (ChildWorkflowExecutionTerminated == getType()) { return "Child Workflow Execution Terminated"; }
        if (SignalExternalWorkflowExecutionInitiated == getType()) { return null; }
        if (SignalExternalWorkflowExecutionFailed == getType()) { return "Signal External Workflow Execution Failed"; }
        if (ExternalWorkflowExecutionSignaled == getType()) { return null; }
        if (RequestCancelExternalWorkflowExecutionInitiated == getType()) { return null; }
        if (RequestCancelExternalWorkflowExecutionFailed == getType()) { return "Request Cancel External Workflow Execution Failed"; }
        if (ExternalWorkflowExecutionCancelRequested == getType()) { return null; }
        throw new IllegalArgumentException("Unknown EventType " + getType());
    }

    public String getDetails() {
        if (WorkflowExecutionStarted == getType()) { return null; }
        if (WorkflowExecutionCancelRequested == getType()) { return historyEvent.getWorkflowExecutionCancelRequestedEventAttributes().getCause(); }
        if (WorkflowExecutionCompleted == getType()) { return null; }
        if (CompleteWorkflowExecutionFailed == getType()) { return historyEvent.getCompleteWorkflowExecutionFailedEventAttributes().getCause(); }
        if (WorkflowExecutionFailed == getType()) { return historyEvent.getWorkflowExecutionFailedEventAttributes().getDetails(); }
        if (FailWorkflowExecutionFailed == getType()) { return historyEvent.getFailWorkflowExecutionFailedEventAttributes().getCause(); }
        if (WorkflowExecutionTimedOut == getType()) { return null; }
        if (WorkflowExecutionCanceled == getType()) { return historyEvent.getWorkflowExecutionCanceledEventAttributes().getDetails(); }
        if (CancelWorkflowExecutionFailed == getType()) { return historyEvent.getCancelWorkflowExecutionFailedEventAttributes().getCause(); }
        if (WorkflowExecutionContinuedAsNew == getType()) { return null; }
        if (ContinueAsNewWorkflowExecutionFailed == getType()) { return historyEvent.getContinueAsNewWorkflowExecutionFailedEventAttributes().getCause(); }
        if (WorkflowExecutionTerminated == getType()) { return historyEvent.getWorkflowExecutionTerminatedEventAttributes().getDetails(); }
        if (DecisionTaskScheduled == getType()) { return null; }
        if (DecisionTaskStarted == getType()) { return null; }
        if (DecisionTaskCompleted == getType()) { return null; }
        if (DecisionTaskTimedOut == getType()) { return null; }
        if (ActivityTaskScheduled == getType()) { return null; }
        if (ScheduleActivityTaskFailed == getType()) { return historyEvent.getScheduleActivityTaskFailedEventAttributes().getCause(); }
        if (ActivityTaskStarted == getType()) { return null; }
        if (ActivityTaskCompleted == getType()) { return null; }
        if (ActivityTaskFailed == getType()) { return historyEvent.getActivityTaskFailedEventAttributes().getDetails(); }
        if (ActivityTaskTimedOut == getType()) { return historyEvent.getActivityTaskTimedOutEventAttributes().getDetails(); }
        if (ActivityTaskCanceled == getType()) { return historyEvent.getActivityTaskCanceledEventAttributes().getDetails(); }
        if (ActivityTaskCancelRequested == getType()) { return null; }
        if (RequestCancelActivityTaskFailed == getType()) { return historyEvent.getRequestCancelActivityTaskFailedEventAttributes().getCause(); }
        if (WorkflowExecutionSignaled == getType()) { return null; }
        if (MarkerRecorded == getType()) { return historyEvent.getMarkerRecordedEventAttributes().getDetails(); }
        if (RecordMarkerFailed == getType()) { return historyEvent.getRecordMarkerFailedEventAttributes().getCause(); }
        if (TimerStarted == getType()) { return null; }
        if (StartTimerFailed == getType()) { return historyEvent.getStartTimerFailedEventAttributes().getCause(); }
        if (TimerFired == getType()) { return null; }
        if (TimerCanceled == getType()) { return null; }
        if (CancelTimerFailed == getType()) { return null; }
        if (StartChildWorkflowExecutionInitiated == getType()) { return null; }
        if (StartChildWorkflowExecutionFailed == getType()) { return historyEvent.getStartChildWorkflowExecutionFailedEventAttributes().getCause(); }
        if (ChildWorkflowExecutionStarted == getType()) { return null; }
        if (ChildWorkflowExecutionCompleted == getType()) { return null; }
        if (ChildWorkflowExecutionFailed == getType()) { return historyEvent.getChildWorkflowExecutionFailedEventAttributes().getDetails(); }
        if (ChildWorkflowExecutionTimedOut == getType()) { return historyEvent.getChildWorkflowExecutionTimedOutEventAttributes().getTimeoutType(); }
        if (ChildWorkflowExecutionCanceled == getType()) { return historyEvent.getChildWorkflowExecutionCanceledEventAttributes().getDetails(); }
        if (ChildWorkflowExecutionTerminated == getType()) { return historyEvent.getChildWorkflowExecutionTerminatedEventAttributes().getWorkflowExecution().getRunId(); }
        if (SignalExternalWorkflowExecutionInitiated == getType()) { return null; }
        if (SignalExternalWorkflowExecutionFailed == getType()) { return historyEvent.getSignalExternalWorkflowExecutionFailedEventAttributes().getCause(); }
        if (ExternalWorkflowExecutionSignaled == getType()) { return null; }
        if (RequestCancelExternalWorkflowExecutionInitiated == getType()) { return null; }
        if (RequestCancelExternalWorkflowExecutionFailed == getType()) { return historyEvent.getRequestCancelExternalWorkflowExecutionFailedEventAttributes().getCause(); }
        if (ExternalWorkflowExecutionCancelRequested == getType()) { return null; }
        throw new IllegalArgumentException("Unknown EventType " + getType());
    }


    public boolean equals(Object o) {
        return o != null && getClass().equals(o.getClass()) && historyEvent.equals(o);
    }

    public int hashCode() {
        return historyEvent.hashCode();
    }

    @Override
    public String toString() {
        return format("%s: %s, %s, %s, %s", getType(), getEventId(), getInitialEventId(), getActionId(), getEventTimestamp());
    }

    /**
     * Sort by eventId descending (most recent event first).
     */
    public int compareTo(Event event) {
        return -getEventId().compareTo(event.getEventId());
    }
}

