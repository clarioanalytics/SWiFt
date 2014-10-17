package com.clario.swift.event.script

import com.amazonaws.services.simpleworkflow.model.EventType

import static com.amazonaws.services.simpleworkflow.model.EventType.*
import static com.clario.swift.event.EventCategory.*
import static com.clario.swift.event.EventState.*

/**
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
import org.joda.time.DateTime;

import static com.amazonaws.services.simpleworkflow.model.EventType.*;
import static com.clario.swift.event.EventCategory.*;
import static com.clario.swift.event.EventState.*;
import static java.lang.String.format;


/**
 * Generated.  Could use reflection but speed is speed.
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
                    def returnValue = map[methodName]
                    if (returnValue == null) {
                        returnValue = calcReturnValueExists(eventType, methodName)
                        if (returnValue) {
                            println "$eventType,  $returnValue"
                        }
                    }
                    if (methodName == 'isInitialAction') {
                        returnValue = returnValue ?: 'false'
                    }

                    pw.print '        '
                    if (['category', 'state', 'isInitialAction'].contains(methodName)) {
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

                pw.println "    }"


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

    public static final Map<String, String> METHOD_NAMES = ['category'       : 'EventCategory',
                                                            'state'          : 'EventState',
                                                            'initialEventId' : 'Long',
                                                            'isInitialAction': 'boolean',
                                                            'actionId'       : 'String',
                                                            'input'          : 'String',
                                                            'control'        : 'String',
                                                            'output'         : 'String',
                                                            'reason'         : 'String',
                                                            'details'        : 'String'].asImmutable();

    static final def EVENT_TYPE_MAP = [
// Activity Events
(ActivityTaskScheduled)                          : [category: ACTION, state: ACTIVE, input: 'input', contorl: 'control', initialEventId: 'eventId', actionId: 'ActivityId', isInitialAction: true],
(ActivityTaskStarted)                            : [category: ACTION, state: ACTIVE, initialEventId: 'scheduledEventId'],
(ActivityTaskCompleted)                          : [category: ACTION, state: SUCCESS, output: 'result', initialEventId: 'scheduledEventId'],
(ActivityTaskCanceled)                           : [category: ACTION, state: ERROR, reason: ActivityTaskCanceled, details: 'details', initialEventId: 'scheduledEventId'],
(ActivityTaskFailed)                             : [category: ACTION, state: ERROR, reason: 'reason', details: 'details', initialEventId: 'scheduledEventId'],
(ActivityTaskTimedOut)                           : [category: ACTION, state: ERROR, reason: 'timeoutType', details: 'details', initialEventId: 'scheduledEventId'],
(ScheduleActivityTaskFailed)                     : [category: ACTION, state: ERROR, reason: ScheduleActivityTaskFailed, details: 'cause', initialEventId: 'eventId'],

// Timers
(TimerStarted)                                   : [category: ACTION, state: ACTIVE, input: TimerStarted, control: 'control', initialEventId: 'eventId', actionId: 'TimerId', isInitialAction: true],
(TimerFired)                                     : [category: ACTION, state: SUCCESS, output: TimerFired, initialEventId: 'StartedEventId', actionId: 'TimerId'],
(TimerCanceled)                                  : [category: ACTION, state: SUCCESS, output: TimerCanceled, initialEventId: 'StartedEventId', actionId: 'TimerId'],
(StartTimerFailed)                               : [category: ACTION, state: ERROR, reason: StartTimerFailed, details: 'cause', actionId: 'TimerId'],

// Child Workflows
(StartChildWorkflowExecutionInitiated)           : [category: ACTION, state: ACTIVE, input: 'input', control: 'control', initialEventId: 'eventId', actionId: 'workflowId', isInitialAction: true],
(ChildWorkflowExecutionStarted)                  : [category: ACTION, state: ACTIVE, initialEventId: 'initiatedEventId'],
(ChildWorkflowExecutionCompleted)                : [category: ACTION, state: SUCCESS, output: 'result', initialEventId: 'initiatedEventId'],
(ChildWorkflowExecutionCanceled)                 : [category: ACTION, state: ERROR, reason: ChildWorkflowExecutionCanceled, details: 'details', initialEventId: 'initiatedEventId'],
(ChildWorkflowExecutionFailed)                   : [category: ACTION, state: ERROR, reason: 'reason', details: 'details', initialEventId: 'initiatedEventId'],
(ChildWorkflowExecutionTerminated)               : [category: ACTION, state: ERROR, reason: ChildWorkflowExecutionTerminated, details: 'runId', initialEventId: 'initiatedEventId'],
(ChildWorkflowExecutionTimedOut)                 : [category: ACTION, state: ERROR, reason: ChildWorkflowExecutionTimedOut, details: 'timeoutType', initialEventId: 'initiatedEventId'],
(StartChildWorkflowExecutionFailed)              : [category: ACTION, state: ERROR, reason: StartChildWorkflowExecutionFailed, details: 'cause', initialEventId: 'initiatedEventId'],

// Signal External Workflows
(SignalExternalWorkflowExecutionInitiated)       : [category: ACTION, state: ACTIVE, input: 'input', initialEventId: 'eventId', actionId: 'SignalName', isInitialAction: true],
(ExternalWorkflowExecutionSignaled)              : [category: ACTION, state: SUCCESS, output: 'runId', initialEventId: 'initiatedEventId'],
(SignalExternalWorkflowExecutionFailed)          : [category: ACTION, state: ERROR, reason: SignalExternalWorkflowExecutionFailed, details: 'cause', initialEventId: 'initiatedEventId'],

// Signal Received (either from this workflow or external source)
(WorkflowExecutionSignaled)                      : [category: SIGNAL, state: SUCCESS, output: 'input', initialEventId: 'eventId', actionId: 'SignalName'],

// Marker
(MarkerRecorded)                                 : [category: ACTION, state: SUCCESS, output: 'details', initialEventId: 'eventId', actionId: 'MarkerName', isInitialAction: true],
(RecordMarkerFailed)                             : [category: ACTION, state: ERROR, reason: RecordMarkerFailed, details: 'cause', initialEventId: 'eventId'],

// Decision Task Events
(DecisionTaskCompleted)                          : [category: DECISION, state: SUCCESS, output: 'ExecutionContext', initialEventId: 'scheduledEventId'],

// WorkflowExecutionStarted
(WorkflowExecutionStarted)                       : [category: WORKFLOW, state: SUCCESS, output: 'input', initialEventId: 'eventId'],

// Workflow Error Events
(WorkflowExecutionCancelRequested)               : [category: WORKFLOW, state: CRITICAL, reason: WorkflowExecutionCancelRequested, details: 'cause', initialEventId: 'eventId'],
(CompleteWorkflowExecutionFailed)                : [category: WORKFLOW, state: CRITICAL, reason: CompleteWorkflowExecutionFailed, details: 'cause', initialEventId: 'eventId'],
(WorkflowExecutionFailed)                        : [category: WORKFLOW, state: CRITICAL, reason: 'reason', details: 'details', initialEventId: 'eventId'],
(FailWorkflowExecutionFailed)                    : [category: WORKFLOW, state: CRITICAL, reason: FailWorkflowExecutionFailed, details: 'cause', initialEventId: 'eventId'],
(CancelWorkflowExecutionFailed)                  : [category: WORKFLOW, state: CRITICAL, reason: CancelWorkflowExecutionFailed, details: 'cause', initialEventId: 'eventId'],
(ContinueAsNewWorkflowExecutionFailed)           : [category: WORKFLOW, state: CRITICAL, reason: ContinueAsNewWorkflowExecutionFailed, details: 'cause', initialEventId: 'eventId'],

// Informational EventTypes that are not useful for general decision making
(DecisionTaskScheduled)                          : [category: EXTERNAL, state: INFO, initialEventId: 'eventId'],
(DecisionTaskStarted)                            : [category: EXTERNAL, state: INFO, initialEventId: 'eventId'],
(DecisionTaskTimedOut)                           : [category: EXTERNAL, state: INFO, initialEventId: 'eventId'],
(WorkflowExecutionCompleted)                     : [category: EXTERNAL, state: INFO, output: 'result', initialEventId: 'eventId'],
(WorkflowExecutionTimedOut)                      : [category: EXTERNAL, state: INFO, initialEventId: 'eventId'],
(WorkflowExecutionCanceled)                      : [category: EXTERNAL, state: INFO, reason: WorkflowExecutionCanceled, details: 'details', initialEventId: 'eventId'],
(WorkflowExecutionContinuedAsNew)                : [category: EXTERNAL, state: INFO, input: 'input', initialEventId: 'eventId'],
(WorkflowExecutionTerminated)                    : [category: EXTERNAL, state: INFO, reason: 'reason', details: 'details', initialEventId: 'eventId'],
(ActivityTaskCancelRequested)                    : [category: EXTERNAL, state: INFO, initialEventId: 'eventId'],
(RequestCancelActivityTaskFailed)                : [category: EXTERNAL, state: INFO, reason: RequestCancelActivityTaskFailed, 'details': 'cause', initialEventId: 'eventId'],
(CancelTimerFailed)                              : [category: EXTERNAL, state: INFO, reason: CancelTimerFailed, 'details': 'cause', initialEventId: 'eventId'],
(RequestCancelExternalWorkflowExecutionInitiated): [category: EXTERNAL, state: INFO, control: 'control', initialEventId: 'eventId'],
(RequestCancelExternalWorkflowExecutionFailed)   : [category: EXTERNAL, state: INFO, reason: RequestCancelExternalWorkflowExecutionFailed, 'details': 'cause', initialEventId: 'eventId'],
(ExternalWorkflowExecutionCancelRequested)       : [category: EXTERNAL, state: INFO, initialEventId: 'eventId']
    ]

}