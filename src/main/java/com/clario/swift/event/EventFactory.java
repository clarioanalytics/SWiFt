package com.clario.swift.event;

import com.amazonaws.services.simpleworkflow.model.*;

/**
 * Generated.  Could use reflection but speed is speed.
 * @author George Coller
 */
class EventFactory {

    // Make all-static utility method
    private EventFactory() { }

    public static Event create(HistoryEvent historyEvent) {
        EventType type = EventType.valueOf(historyEvent.getEventType());
        switch (type) {

            case WorkflowExecutionStarted: return new WorkflowExecutionStartedEvent(historyEvent);
            case WorkflowExecutionCancelRequested: return new WorkflowExecutionCancelRequestedEvent(historyEvent);
            case WorkflowExecutionCompleted: return new WorkflowExecutionCompletedEvent(historyEvent);
            case CompleteWorkflowExecutionFailed: return new CompleteWorkflowExecutionFailedEvent(historyEvent);
            case WorkflowExecutionFailed: return new WorkflowExecutionFailedEvent(historyEvent);
            case FailWorkflowExecutionFailed: return new FailWorkflowExecutionFailedEvent(historyEvent);
            case WorkflowExecutionTimedOut: return new WorkflowExecutionTimedOutEvent(historyEvent);
            case WorkflowExecutionCanceled: return new WorkflowExecutionCanceledEvent(historyEvent);
            case CancelWorkflowExecutionFailed: return new CancelWorkflowExecutionFailedEvent(historyEvent);
            case WorkflowExecutionContinuedAsNew: return new WorkflowExecutionContinuedAsNewEvent(historyEvent);
            case ContinueAsNewWorkflowExecutionFailed: return new ContinueAsNewWorkflowExecutionFailedEvent(historyEvent);
            case WorkflowExecutionTerminated: return new WorkflowExecutionTerminatedEvent(historyEvent);
            case DecisionTaskScheduled: return new DecisionTaskScheduledEvent(historyEvent);
            case DecisionTaskStarted: return new DecisionTaskStartedEvent(historyEvent);
            case DecisionTaskCompleted: return new DecisionTaskCompletedEvent(historyEvent);
            case DecisionTaskTimedOut: return new DecisionTaskTimedOutEvent(historyEvent);
            case ActivityTaskScheduled: return new ActivityTaskScheduledEvent(historyEvent);
            case ScheduleActivityTaskFailed: return new ScheduleActivityTaskFailedEvent(historyEvent);
            case ActivityTaskStarted: return new ActivityTaskStartedEvent(historyEvent);
            case ActivityTaskCompleted: return new ActivityTaskCompletedEvent(historyEvent);
            case ActivityTaskFailed: return new ActivityTaskFailedEvent(historyEvent);
            case ActivityTaskTimedOut: return new ActivityTaskTimedOutEvent(historyEvent);
            case ActivityTaskCanceled: return new ActivityTaskCanceledEvent(historyEvent);
            case ActivityTaskCancelRequested: return new ActivityTaskCancelRequestedEvent(historyEvent);
            case RequestCancelActivityTaskFailed: return new RequestCancelActivityTaskFailedEvent(historyEvent);
            case WorkflowExecutionSignaled: return new WorkflowExecutionSignaledEvent(historyEvent);
            case MarkerRecorded: return new MarkerRecordedEvent(historyEvent);
            case RecordMarkerFailed: return new RecordMarkerFailedEvent(historyEvent);
            case TimerStarted: return new TimerStartedEvent(historyEvent);
            case StartTimerFailed: return new StartTimerFailedEvent(historyEvent);
            case TimerFired: return new TimerFiredEvent(historyEvent);
            case TimerCanceled: return new TimerCanceledEvent(historyEvent);
            case CancelTimerFailed: return new CancelTimerFailedEvent(historyEvent);
            case StartChildWorkflowExecutionInitiated: return new StartChildWorkflowExecutionInitiatedEvent(historyEvent);
            case StartChildWorkflowExecutionFailed: return new StartChildWorkflowExecutionFailedEvent(historyEvent);
            case ChildWorkflowExecutionStarted: return new ChildWorkflowExecutionStartedEvent(historyEvent);
            case ChildWorkflowExecutionCompleted: return new ChildWorkflowExecutionCompletedEvent(historyEvent);
            case ChildWorkflowExecutionFailed: return new ChildWorkflowExecutionFailedEvent(historyEvent);
            case ChildWorkflowExecutionTimedOut: return new ChildWorkflowExecutionTimedOutEvent(historyEvent);
            case ChildWorkflowExecutionCanceled: return new ChildWorkflowExecutionCanceledEvent(historyEvent);
            case ChildWorkflowExecutionTerminated: return new ChildWorkflowExecutionTerminatedEvent(historyEvent);
            case SignalExternalWorkflowExecutionInitiated: return new SignalExternalWorkflowExecutionInitiatedEvent(historyEvent);
            case SignalExternalWorkflowExecutionFailed: return new SignalExternalWorkflowExecutionFailedEvent(historyEvent);
            case ExternalWorkflowExecutionSignaled: return new ExternalWorkflowExecutionSignaledEvent(historyEvent);
            case RequestCancelExternalWorkflowExecutionInitiated: return new RequestCancelExternalWorkflowExecutionInitiatedEvent(historyEvent);
            case RequestCancelExternalWorkflowExecutionFailed: return new RequestCancelExternalWorkflowExecutionFailedEvent(historyEvent);
            case ExternalWorkflowExecutionCancelRequested: return new ExternalWorkflowExecutionCancelRequestedEvent(historyEvent);

            default: throw new IllegalStateException("Unknown EventType " + type);
        }
    }
}

