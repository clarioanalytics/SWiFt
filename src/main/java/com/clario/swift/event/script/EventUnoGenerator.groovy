package com.clario.swift.event.script

import com.amazonaws.services.simpleworkflow.model.EventType

import static com.amazonaws.services.simpleworkflow.model.EventType.*
import static com.clario.swift.TaskType.*
import static com.clario.swift.event.EventState.*

/**
 * Generates {@link com.clario.swift.event.Event} class.
 * @author George Coller
 */
public class EventUnoGenerator {

    public static final String PACKAGE = "/Workspace/bitbucket/services-swift/src/main/java/com/clario/swift/event/"

    public static String capitalize(String str) {
        return str ? str[0].capitalize() + str.substring(1) : str
    }

    public static String makeGetter(Object o) {
        if (o instanceof String || o instanceof GString) {
            String str = o;
            return str ? "${str.startsWith('is') ? str : 'get' + capitalize(str)}()" : null
        } else if (o instanceof EventType) {
            EventType eventType = o;
            return '"' + eventType + '"'
        }
        return null;
    }

    public static String addSpaces(String str) {
        return str.replaceAll(/([A-Z])/, ' $1').trim()
    }

    public static void main(String[] args) {
        createEventClass()
    }


    def static void createEventClass() {
        String factoryName = PACKAGE + "Event.java"

        new File(factoryName).withPrintWriter { pw ->

            pw.println """package com.clario.swift.event;

import com.amazonaws.services.simpleworkflow.model.EventType;
import com.amazonaws.services.simpleworkflow.model.HistoryEvent;
import com.clario.swift.TaskType;
import org.joda.time.DateTime;

import static com.amazonaws.services.simpleworkflow.model.EventType.*;
import static com.clario.swift.TaskType.*;
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
"""
            METHOD_NAMES.each { String methodName, String returnType ->

                pw.println "    public $returnType ${makeGetter(methodName)} {"
                EventType.each { EventType eventType ->
                    def map = EVENT_TYPE_MAP[eventType]

                    def returnValue
                    try {
                        returnValue = map[methodName] ?: calcReturnValueExists(eventType, methodName)
                    } catch (e) {
                        println returnValue
                    }
                    if (methodName == 'initialEventId' && INITIAL == map.state) {
                        returnValue = 'eventId'
                    }

                    pw.print '        '
                    if (['task', 'state'].contains(methodName)) {
                        pw.println "if ($eventType == getType()) { return $returnValue; }"
                    } else if (returnValue == null) {
                        pw.println "if ($eventType == getType()) { return null; }"
                    } else if (returnValue instanceof EventType) {
                        pw.println "if ($eventType == getType()) { return \"${addSpaces(returnValue.toString())}\"; }"
                    } else if (['eventId'].contains(returnValue)) {
                        pw.println "if ($eventType == getType()) { return historyEvent.${makeGetter(returnValue)}; }"
                    } else if (['runId'].contains(returnValue)) {
                        pw.println "if ($eventType == getType()) { return historyEvent.${makeGetter("${eventType}EventAttributes")}.getWorkflowExecution().${makeGetter(returnValue)}; }"
                    } else {
                        pw.println "if ($eventType == getType()) { return historyEvent.${makeGetter("${eventType}EventAttributes")}.${makeGetter(returnValue)}; }"
                    }

                }
                pw.println '        throw new IllegalArgumentException("Unknown EventType " + getType());'

                pw.println "    }\n"


            }

            pw.println """
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
"""
        }
    }

    static def calcReturnValueExists(EventType eventType, String fieldName) {
        Class clazz = Class.forName("com.amazonaws.services.simpleworkflow.model.${eventType}EventAttributes")
        return clazz.privateGetDeclaredFields(false).find { it.name == fieldName } == null ? null : fieldName
    }

    public static final Map<String, String> METHOD_NAMES = ['task'          : 'TaskType',
                                                            'state'         : 'EventState',
                                                            'initialEventId': 'Long',
                                                            'actionId'      : 'String',
                                                            'input'         : 'String',
                                                            'control'       : 'String',
                                                            'output'        : 'String',
                                                            'reason'        : 'String',
                                                            'details'       : 'String'].asImmutable();

    static final def EVENT_TYPE_MAP = [
// Activity Events
(ActivityTaskScheduled)                          : [task: ACTIVITY, state: INITIAL, actionId: 'activityId'],
(ActivityTaskStarted)                            : [task: ACTIVITY, state: ACTIVE, initialEventId: 'scheduledEventId'],
(ActivityTaskCompleted)                          : [task: ACTIVITY, state: SUCCESS, output: 'result', initialEventId: 'scheduledEventId'],
(ActivityTaskCanceled)                           : [task: ACTIVITY, state: ERROR, reason: ActivityTaskCanceled, initialEventId: 'scheduledEventId'],
(ActivityTaskFailed)                             : [task: ACTIVITY, state: ERROR, initialEventId: 'scheduledEventId'],
(ActivityTaskTimedOut)                           : [task: ACTIVITY, state: ERROR, reason: 'timeoutType', initialEventId: 'scheduledEventId'],
(ScheduleActivityTaskFailed)                     : [task: ACTIVITY, state: ERROR, reason: ScheduleActivityTaskFailed, details: 'cause', initialEventId: 'eventId'],
(ActivityTaskCancelRequested)                    : [task: ACTIVITY, state: ERROR, reason: ActivityTaskCancelRequested, initialEventId: 'eventId'],
(RequestCancelActivityTaskFailed)                : [task: ACTIVITY, state: ERROR, reason: RequestCancelActivityTaskFailed, 'details': 'cause', initialEventId: 'eventId'],

// Timers
(TimerStarted)                                   : [task: TIMER, state: INITIAL, input: TimerStarted, initialEventId: 'eventId', actionId: 'timerId'],
(CancelTimerFailed)                              : [task: TIMER, state: ACTIVE, initialEventId: 'eventId', actionId: 'timerId'],
(TimerFired)                                     : [task: TIMER, state: SUCCESS, output: TimerFired, initialEventId: 'StartedEventId', actionId: 'timerId'],
(TimerCanceled)                                  : [task: TIMER, state: SUCCESS, output: TimerCanceled, initialEventId: 'StartedEventId', actionId: 'timerId'],
(StartTimerFailed)                               : [task: TIMER, state: ERROR, reason: StartTimerFailed, details: 'cause', actionId: 'timerId'],

// Child Workflows
(StartChildWorkflowExecutionInitiated)           : [task: START_CHILD_WORKFLOW, state: INITIAL, actionId: 'workflowId'],
(ChildWorkflowExecutionStarted)                  : [task: START_CHILD_WORKFLOW, state: ACTIVE, initialEventId: 'initiatedEventId'],
(ChildWorkflowExecutionCompleted)                : [task: START_CHILD_WORKFLOW, state: SUCCESS, output: 'result', initialEventId: 'initiatedEventId'],
(ChildWorkflowExecutionCanceled)                 : [task: START_CHILD_WORKFLOW, state: ERROR, reason: ChildWorkflowExecutionCanceled, initialEventId: 'initiatedEventId'],
(ChildWorkflowExecutionFailed)                   : [task: START_CHILD_WORKFLOW, state: ERROR, initialEventId: 'initiatedEventId'],
(ChildWorkflowExecutionTerminated)               : [task: START_CHILD_WORKFLOW, state: ERROR, reason: ChildWorkflowExecutionTerminated, details: 'runId', initialEventId: 'initiatedEventId'],
(ChildWorkflowExecutionTimedOut)                 : [task: START_CHILD_WORKFLOW, state: ERROR, reason: ChildWorkflowExecutionTimedOut, details: 'timeoutType', initialEventId: 'initiatedEventId'],
(StartChildWorkflowExecutionFailed)              : [task: START_CHILD_WORKFLOW, state: ERROR, reason: StartChildWorkflowExecutionFailed, details: 'cause', initialEventId: 'initiatedEventId'],

// Action: Record Marker
(MarkerRecorded)                                 : [task: RECORD_MARKER, state: INITIAL, input: 'details', output: 'details', initialEventId: 'eventId', actionId: 'MarkerName'],
(RecordMarkerFailed)                             : [task: RECORD_MARKER, state: ERROR, reason: RecordMarkerFailed, details: 'cause', initialEventId: 'eventId'],

// Signal External Workflows
(SignalExternalWorkflowExecutionInitiated)       : [task: SIGNAL_EXTERNAL_WORKFLOW, state: INITIAL, actionId: 'SignalName'],
(ExternalWorkflowExecutionSignaled)              : [task: SIGNAL_EXTERNAL_WORKFLOW, state: SUCCESS, output: 'runId', initialEventId: 'initiatedEventId'],
(SignalExternalWorkflowExecutionFailed)          : [task: SIGNAL_EXTERNAL_WORKFLOW, state: ERROR, reason: SignalExternalWorkflowExecutionFailed, details: 'cause', initialEventId: 'initiatedEventId'],

// Action: External Workflow Cancel
(RequestCancelExternalWorkflowExecutionInitiated): [task: CANCEL_EXTERNAL_WORKFLOW, state: INITIAL, actionId: 'control'],
(ExternalWorkflowExecutionCancelRequested)       : [task: CANCEL_EXTERNAL_WORKFLOW, state: SUCCESS, initialEventId: 'eventId'],
(RequestCancelExternalWorkflowExecutionFailed)   : [task: CANCEL_EXTERNAL_WORKFLOW, state: ERROR, actionId: 'control', reason: RequestCancelExternalWorkflowExecutionFailed, 'details': 'cause', initialEventId: 'eventId'],

// Decision Task Events
(DecisionTaskScheduled)                          : [task: DECISION, state: INITIAL],
(DecisionTaskStarted)                            : [task: DECISION, state: ACTIVE, initialEventId: 'eventId'],
(DecisionTaskCompleted)                          : [task: DECISION, state: SUCCESS, output: 'executionContext', initialEventId: 'scheduledEventId'],
(DecisionTaskTimedOut)                           : [task: DECISION, state: ERROR, initialEventId: 'eventId'],

// WorkflowExecutionStarted
(WorkflowExecutionStarted)                       : [task: WORKFLOW_EXECUTION, state: INITIAL],
(WorkflowExecutionCompleted)                     : [task: WORKFLOW_EXECUTION, state: SUCCESS, output: 'result', initialEventId: 'eventId'],
(WorkflowExecutionCancelRequested)               : [task: WORKFLOW_EXECUTION, state: ACTIVE, reason: WorkflowExecutionCancelRequested, details: 'cause', initialEventId: 'eventId'],
(FailWorkflowExecutionFailed)                    : [task: WORKFLOW_EXECUTION, state: ERROR, reason: FailWorkflowExecutionFailed, details: 'cause', initialEventId: 'eventId'],
(WorkflowExecutionFailed)                        : [task: WORKFLOW_EXECUTION, state: ERROR, reason: WorkflowExecutionFailed, initialEventId: 'eventId'],
(WorkflowExecutionTimedOut)                      : [task: WORKFLOW_EXECUTION, state: ERROR, reason: WorkflowExecutionTimedOut, initialEventId: 'eventId'],
(WorkflowExecutionCanceled)                      : [task: WORKFLOW_EXECUTION, state: ERROR, reason: WorkflowExecutionCanceled, initialEventId: 'eventId'],
(WorkflowExecutionTerminated)                    : [task: WORKFLOW_EXECUTION, state: ERROR, reason: WorkflowExecutionTerminated, initialEventId: 'eventId'],
(CompleteWorkflowExecutionFailed)                : [task: WORKFLOW_EXECUTION, state: ERROR, reason: CompleteWorkflowExecutionFailed, details: 'cause', initialEventId: 'eventId'],
(CancelWorkflowExecutionFailed)                  : [task: WORKFLOW_EXECUTION, state: ERROR, reason: CancelWorkflowExecutionFailed, details: 'cause', initialEventId: 'eventId'],

(WorkflowExecutionContinuedAsNew)                : [task: CONTINUE_AS_NEW, state: INITIAL],
(ContinueAsNewWorkflowExecutionFailed)           : [task: CONTINUE_AS_NEW, state: ERROR, reason: ContinueAsNewWorkflowExecutionFailed, details: 'cause', initialEventId: 'eventId'],

// Signal Received (either from this workflow or external source)
(WorkflowExecutionSignaled)                      : [task: WORKFLOW_SIGNALED, state: SUCCESS, output: 'input', initialEventId: 'eventId', actionId: 'SignalName'],

(LambdaFunctionScheduled)                        : [task: LAMBDA, state: INITIAL, output: 'input'],
(LambdaFunctionStarted)                          : [task: LAMBDA, state: ACTIVE, initialEventId: 'scheduledEventId'],
(LambdaFunctionCompleted)                        : [task: LAMBDA, state: SUCCESS, output: 'result', initialEventId: 'scheduledEventId'],
(LambdaFunctionFailed)                           : [task: LAMBDA, state: ERROR, reason: 'reason', details: 'details', initialEventId: 'scheduledEventId'],
(LambdaFunctionTimedOut)                         : [task: LAMBDA, state: ERROR, reason: LambdaFunctionTimedOut, details: 'timeoutType', initialEventId: 'scheduledEventId'],
(ScheduleLambdaFunctionFailed)                   : [task: LAMBDA, state: ERROR, reason: ScheduleLambdaFunctionFailed, control: 'name', details: 'cause'],
(StartLambdaFunctionFailed)                      : [task: LAMBDA, state: ERROR, reason: StartLambdaFunctionFailed, output: 'message', details: 'cause', initialEventId: 'scheduledEventId'],

    ]

}