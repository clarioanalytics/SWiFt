package com.clario.swift.event.script

import com.amazonaws.services.simpleworkflow.model.*
import com.clario.swift.event.Event
import com.clario.swift.event.EventCategory
import com.clario.swift.event.EventState

import java.lang.reflect.Field

import static com.amazonaws.services.simpleworkflow.model.EventType.*
import static com.clario.swift.event.EventCategory.*
import static com.clario.swift.event.EventState.*

/**
 * @author George Coller
 */
public class EventPojoGenerator {

    public static final List<String> IO_FIELD_NAMES = ['input', 'control', 'output', 'reason', 'details'].asImmutable();
    public static final String PACKAGE = "/Workspace/bitbucket/services-swift/src/main/java/com/clario/swift/event/"
    public static final String EVENT_STATE = EventState.simpleName
    public static final String EVENT_CATEGORY = EventCategory.simpleName

    public static String capitalize(String str) {
        return str ? str[0].capitalize() + str.substring(1) : str
    }

    public static String makeGetter(Object o) {
        if (o instanceof String) {
            String str = o;
            return str ? "get${capitalize(str)}()" : null
        } else if (o instanceof EventType) {
            EventType eventType = o;
            return '"' + eventType + '"'
        }
        return null;
    }

    public static void main(String[] args) {
        createFactoryClass()

        EventType.each { EventType type ->
            String baseClassName = Event.class.simpleName
            String className = type.name() + "Event"
            String fileName = PACKAGE + className + ".java"

            def map = EVENT_TYPE_MAP[type]
            if (map == null) {
                println "missing definition for type: $type"
                return
            }

            new File(fileName).withPrintWriter { PrintWriter pw ->
                pw.println """package com.clario.swift.event;

import com.amazonaws.services.simpleworkflow.model.*;

/**
 * @author George Coller
 */
public class $className extends ${baseClassName} {

    protected $className(HistoryEvent historyEvent) {
        super(historyEvent);
    }

    @Override public $EVENT_STATE getState() { return $EVENT_STATE.${map.state}; }

    @Override public $EVENT_CATEGORY getCategory() { return $EVENT_CATEGORY.${map.category}; }

    @Override public Long getInitialEventId() { return ${makeGetter(map.initialEventId)}; }

    @Override public boolean isInitialAction() { return ${map.isStartAction ?: false}; }

    @Override public String getActionId() { return ${makeGetter(map.actionId)}; }
"""

                IO_FIELD_NAMES.each {
                    if (map[it]) {
                        pw.println """    @Override public String ${makeGetter(it)} {  return ${makeGetter(map[it])}; }\n"""
                    }
                }


                pw.println """    public ${className}Attributes getAttributes() {return historyEvent.get${className}Attributes();}\n"""
                def clazz = Class.forName("com.amazonaws.services.simpleworkflow.model.${className}Attributes")
                recurse(map, pw, null, clazz)

                pw.println("}")
            }
        }
    }


    static void recurse(Map<String, Object> map, PrintWriter pw, Class parentClazz, Class clazz) {
        clazz.privateGetDeclaredFields(false).findAll {
            return !(['serialVersionUID', 'autoConstruct'].contains(it.name) || map.containsKey(it.name))
        }.each { Field field ->
            if (field.type.name.startsWith('com.amazonaws')) {
                recurse(map, pw, clazz, Class.forName(field.type.name))
            } else {
                String returnType = field.type.name.replaceFirst('java.lang.', '')
                String fieldName = capitalize(field.name)
                String attrsFieldName = parentClazz ? clazz?.simpleName : ""
                String accessorMethodName = "$attrsFieldName$fieldName"

                // Tasteful renaming from AmazonSWF
                if (WorkflowExecution == clazz) {
                    if ([WorkflowExecutionCancelRequestedEventAttributes, WorkflowExecutionSignaledEventAttributes].contains(parentClazz)) {
                        attrsFieldName = 'External' + attrsFieldName
                    } else if (parentClazz == WorkflowExecutionStartedEventAttributes) {
                        attrsFieldName = 'Parent' + attrsFieldName
                    }
                    accessorMethodName = fieldName
                } else if (WorkflowType == clazz) {
                    accessorMethodName = "Workflow$fieldName"
                } else if (TaskList == clazz) {
                    accessorMethodName = clazz.simpleName
                }

                String override = IO_FIELD_NAMES.contains(accessorMethodName.toLowerCase()) ? "@Override " : "";
                String returnStatement = parentClazz ? makeGetter(attrsFieldName) + "." + makeGetter(fieldName) : makeGetter(fieldName)
                String statement = ['    public', override, returnType, makeGetter(accessorMethodName), "{ return getAttributes().$returnStatement; }"].join(" ")
                pw.println statement
                pw.println ""
            }
        }
    }

    def static void createFactoryClass() {
        String factoryName = PACKAGE + "EventFactory.java"

        new File(factoryName).withPrintWriter { pw ->

            pw.println """package com.clario.swift.event;

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
"""
            EventType.each { EventType type ->
                pw.print "            "
                pw.println "case $type: return new ${type}Event(historyEvent);"
            }
            pw.println """
            default: throw new IllegalStateException("Unknown EventType " + type);
        }
    }
}
"""
        }
    }
    static final def EVENT_TYPE_MAP = [
// Activity Events
(ActivityTaskScheduled)                          : [category: ACTION, state: ACTIVE, initialEventId: 'EventId', actionId: 'ActivityId', isStartAction: true],
(ActivityTaskStarted)                            : [category: ACTION, state: ACTIVE, initialEventId: 'ScheduledEventId'],
(ActivityTaskCompleted)                          : [category: ACTION, state: SUCCESS, output: 'result', initialEventId: 'ScheduledEventId'],
(ActivityTaskCanceled)                           : [category: ACTION, state: ERROR, reason: ActivityTaskCanceled, initialEventId: 'ScheduledEventId'],
(ActivityTaskFailed)                             : [category: ACTION, state: ERROR, initialEventId: 'ScheduledEventId'],
(ActivityTaskTimedOut)                           : [category: ACTION, state: ERROR, reason: 'timeoutType', initialEventId: 'ScheduledEventId'],
(ScheduleActivityTaskFailed)                     : [category: ACTION, state: ERROR, reason: ScheduleActivityTaskFailed, details: 'cause', initialEventId: 'EventId'],

// Timers
(TimerStarted)                                   : [category: ACTION, state: ACTIVE, input: TimerStarted, initialEventId: 'EventId', actionId: 'TimerId', isStartAction: true],
(TimerFired)                                     : [category: ACTION, state: SUCCESS, output: TimerFired, initialEventId: 'StartedEventId', actionId: 'TimerId'],
(TimerCanceled)                                  : [category: ACTION, state: SUCCESS, output: TimerCanceled, initialEventId: 'StartedEventId', actionId: 'TimerId'],
(StartTimerFailed)                               : [category: ACTION, state: ERROR, reason: StartTimerFailed, details: 'cause', actionId: 'TimerId'],

// Child Workflows
(StartChildWorkflowExecutionInitiated)           : [category: ACTION, state: ACTIVE, initialEventId: 'EventId', actionId: 'WorkflowId', isStartAction: true],
(ChildWorkflowExecutionStarted)                  : [category: ACTION, state: ACTIVE, initialEventId: 'InitiatedEventId'],
(ChildWorkflowExecutionCompleted)                : [category: ACTION, state: SUCCESS, output: 'result', initialEventId: 'InitiatedEventId'],
(ChildWorkflowExecutionCanceled)                 : [category: ACTION, state: ERROR, reason: ChildWorkflowExecutionCanceled, initialEventId: 'InitiatedEventId'],
(ChildWorkflowExecutionFailed)                   : [category: ACTION, state: ERROR, initialEventId: 'InitiatedEventId'],
(ChildWorkflowExecutionTerminated)               : [category: ACTION, state: ERROR, reason: ChildWorkflowExecutionTerminated, initialEventId: 'InitiatedEventId'],
(ChildWorkflowExecutionTimedOut)                 : [category: ACTION, state: ERROR, reason: ChildWorkflowExecutionTimedOut, details: 'timeoutType', initialEventId: 'InitiatedEventId'],
(StartChildWorkflowExecutionFailed)              : [category: ACTION, state: ERROR, reason: StartChildWorkflowExecutionFailed, details: 'cause', initialEventId: 'InitiatedEventId'],

// Signal External Workflows
(SignalExternalWorkflowExecutionInitiated)       : [category: ACTION, state: ACTIVE, initialEventId: 'EventId', actionId: 'SignalName', isStartAction: true],
(ExternalWorkflowExecutionSignaled)              : [category: ACTION, state: SUCCESS, output: 'runId', initialEventId: 'InitiatedEventId'],
(SignalExternalWorkflowExecutionFailed)          : [category: ACTION, state: ERROR, reason: SignalExternalWorkflowExecutionFailed, details: 'cause', initialEventId: 'InitiatedEventId'],

// Signal Received (either from this workflow or external source)
(WorkflowExecutionSignaled)                      : [category: SIGNAL, state: SUCCESS, output: 'input', initialEventId: 'EventId', actionId: 'SignalName'],

// Marker
(MarkerRecorded)                                 : [category: ACTION, state: SUCCESS, output: 'details', initialEventId: 'EventId', actionId: 'MarkerName', isStartAction: true],
(RecordMarkerFailed)                             : [category: ACTION, state: ERROR, reason: RecordMarkerFailed, details: 'cause', initialEventId: 'EventId'],

// Decision Task Events
(DecisionTaskCompleted)                          : [category: DECISION, state: SUCCESS, output: 'ExecutionContext', initialEventId: 'ScheduledEventId'],

// WorkflowExecutionStarted
(WorkflowExecutionStarted)                       : [category: WORKFLOW, state: SUCCESS, output: 'input', initialEventId: 'EventId'],

// Workflow Error Events
(WorkflowExecutionCancelRequested)               : [category: WORKFLOW, state: CRITICAL, reason: WorkflowExecutionCancelRequested, details: 'cause', initialEventId: 'EventId'],
(CompleteWorkflowExecutionFailed)                : [category: WORKFLOW, state: CRITICAL, reason: CompleteWorkflowExecutionFailed, details: 'cause', initialEventId: 'EventId'],
(WorkflowExecutionFailed)                        : [category: WORKFLOW, state: CRITICAL, initialEventId: 'EventId'],
(FailWorkflowExecutionFailed)                    : [category: WORKFLOW, state: CRITICAL, reason: FailWorkflowExecutionFailed, details: 'cause', initialEventId: 'EventId'],
(CancelWorkflowExecutionFailed)                  : [category: WORKFLOW, state: CRITICAL, reason: CancelWorkflowExecutionFailed, details: 'cause', initialEventId: 'EventId'],
(ContinueAsNewWorkflowExecutionFailed)           : [category: WORKFLOW, state: CRITICAL, reason: ContinueAsNewWorkflowExecutionFailed, details: 'cause', initialEventId: 'EventId'],

// Informational EventTypes that are not useful for general decision making
(DecisionTaskScheduled)                          : [category: EXTERNAL, state: INFO, initialEventId: 'EventId'],
(DecisionTaskStarted)                            : [category: EXTERNAL, state: INFO, initialEventId: 'EventId'],
(DecisionTaskTimedOut)                           : [category: EXTERNAL, state: INFO, initialEventId: 'EventId'],
(WorkflowExecutionCompleted)                     : [category: EXTERNAL, state: INFO, initialEventId: 'EventId'],
(WorkflowExecutionTimedOut)                      : [category: EXTERNAL, state: INFO, initialEventId: 'EventId'],
(WorkflowExecutionCanceled)                      : [category: EXTERNAL, state: INFO, initialEventId: 'EventId'],
(WorkflowExecutionContinuedAsNew)                : [category: EXTERNAL, state: INFO, initialEventId: 'EventId'],
(WorkflowExecutionTerminated)                    : [category: EXTERNAL, state: INFO, initialEventId: 'EventId'],
(ActivityTaskCancelRequested)                    : [category: EXTERNAL, state: INFO, initialEventId: 'EventId'],
(RequestCancelActivityTaskFailed)                : [category: EXTERNAL, state: INFO, initialEventId: 'EventId'],
(CancelTimerFailed)                              : [category: EXTERNAL, state: INFO, initialEventId: 'EventId'],
(RequestCancelExternalWorkflowExecutionInitiated): [category: EXTERNAL, state: INFO, initialEventId: 'EventId'],
(RequestCancelExternalWorkflowExecutionFailed)   : [category: EXTERNAL, state: INFO, initialEventId: 'EventId'],
(ExternalWorkflowExecutionCancelRequested)       : [category: EXTERNAL, state: INFO, initialEventId: 'EventId']
    ]

}