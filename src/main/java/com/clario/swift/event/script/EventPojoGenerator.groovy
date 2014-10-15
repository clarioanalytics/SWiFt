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

    public static final String PACKAGE = "/Workspace/bitbucket/services-swift/src/main/java/com/clario/swift/event/"
    public static final String EVENT_STATE = EventState.simpleName
    public static final String EVENT_CATEGORY = EventCategory.simpleName

    public static String capitalize(String str) {
        return str ? str[0].capitalize() + str.substring(1) : str
    }

    public static String makeGetter(String str) {
        return str ? "get${capitalize(str)}()" : null
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

    @Override public String getData1() { return ${makeGetter(map.data1)}; }

    @Override public String getData2() { return ${makeGetter(map.data2)}; }

    public ${className}Attributes getAttributes() {return historyEvent.get${className}Attributes();}
"""
                def clazz = Class.forName("com.amazonaws.services.simpleworkflow.model.${className}Attributes")
                recurse(pw, null, clazz)

                pw.println("}")
            }
        }
    }


    static void recurse(PrintWriter pw, Class parentClazz, Class clazz) {
        clazz.privateGetDeclaredFields(false).findAll {
            return !['serialVersionUID', 'autoConstruct'].contains(it.name)
        }.each { Field field ->
            if (field.type.name.startsWith('com.amazonaws')) {
                recurse(pw, clazz, Class.forName(field.type.name))
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


                String returnStatement = parentClazz ? makeGetter(attrsFieldName) + "." + makeGetter(fieldName) : makeGetter(fieldName)
                pw.println "    public $returnType ${makeGetter(accessorMethodName)} { return getAttributes().$returnStatement; }"
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
(ActivityTaskScheduled)                          : [category: ACTION, state: ACTIVE, data1: 'input', data2: 'control', initialEventId: 'EventId', actionId: 'ActivityId', isStartAction: true],
(ActivityTaskStarted)                            : [category: ACTION, state: ACTIVE, data1: 'identity', initialEventId: 'ScheduledEventId'],
(ActivityTaskCompleted)                          : [category: ACTION, state: SUCCESS, data1: 'result', initialEventId: 'ScheduledEventId'],
(ActivityTaskCanceled)                           : [category: ACTION, state: ERROR, data1: 'details', initialEventId: 'ScheduledEventId'],
(ActivityTaskFailed)                             : [category: ACTION, state: ERROR, data1: 'reason', data2: 'details', initialEventId: 'ScheduledEventId'],
(ActivityTaskTimedOut)                           : [category: ACTION, state: ERROR, data1: 'timeoutType', data2: 'details', initialEventId: 'ScheduledEventId'],

// Timers
(TimerStarted)                                   : [category: ACTION, state: ACTIVE, data1: 'control', initialEventId: 'EventId', actionId: 'TimerId', isStartAction: true],
(TimerFired)                                     : [category: ACTION, state: SUCCESS, initialEventId: 'StartedEventId', actionId: 'TimerId'],
(TimerCanceled)                                  : [category: ACTION, state: SUCCESS, initialEventId: 'StartedEventId', actionId: 'TimerId'],
(StartTimerFailed)                               : [category: ACTION, state: ERROR, data1: 'cause', actionId: 'TimerId'],

// Child Workflows
(StartChildWorkflowExecutionInitiated)           : [category: ACTION, state: ACTIVE, data1: 'input', data2: 'control', initialEventId: 'EventId', actionId: 'WorkflowId', isStartAction: true],
(ChildWorkflowExecutionStarted)                  : [category: ACTION, state: ACTIVE, data1: 'runId', initialEventId: 'InitiatedEventId'],
(ChildWorkflowExecutionCompleted)                : [category: ACTION, state: SUCCESS, data1: 'result', initialEventId: 'InitiatedEventId'],
(ChildWorkflowExecutionCanceled)                 : [category: ACTION, state: ERROR, data1: 'details', initialEventId: 'InitiatedEventId'],
(ChildWorkflowExecutionFailed)                   : [category: ACTION, state: ERROR, data1: 'reason', data2: 'details', initialEventId: 'InitiatedEventId'],
(ChildWorkflowExecutionTerminated)               : [category: ACTION, state: ERROR, initialEventId: 'InitiatedEventId'],
(ChildWorkflowExecutionTimedOut)                 : [category: ACTION, state: ERROR, data1: 'timeoutType', initialEventId: 'InitiatedEventId'],

// Signal External Workflows
(SignalExternalWorkflowExecutionInitiated)       : [category: ACTION, state: ACTIVE, data1: 'input', data2: 'control', initialEventId: 'EventId', actionId: 'SignalName', isStartAction: true],
(ExternalWorkflowExecutionSignaled)              : [category: ACTION, state: SUCCESS, data1: 'runId', initialEventId: 'InitiatedEventId'],

// Signal Received (either from this workflow or external source)
(WorkflowExecutionSignaled)                      : [category: SIGNAL, state: SUCCESS, data1: 'input', initialEventId: 'EventId', actionId:'SignalName'],

// Marker
(MarkerRecorded)                                 : [category: ACTION, state: SUCCESS, data1: 'details', initialEventId: 'EventId', actionId: 'MarkerName', isStartAction: true],

// Decision Task Events
(DecisionTaskCompleted)                          : [category: DECISION, state: SUCCESS, data1: 'ExecutionContext', initialEventId: 'ScheduledEventId'],
// WorkflowExecutionStarted
(WorkflowExecutionStarted)                       : [category: WORKFLOW, state: SUCCESS, data1: 'input', initialEventId: 'EventId'],

// Workflow Error Events
(WorkflowExecutionCancelRequested)               : [category: WORKFLOW, state: CRITICAL, data1: 'cause', initialEventId: 'EventId'],
(ScheduleActivityTaskFailed)                     : [category: WORKFLOW, state: CRITICAL, data1: 'cause', data2: 'activityId', initialEventId: 'EventId'],
(StartChildWorkflowExecutionFailed)              : [category: WORKFLOW, state: CRITICAL, data1: 'cause', data2: 'control', initialEventId: 'EventId'],
(SignalExternalWorkflowExecutionFailed)          : [category: WORKFLOW, state: CRITICAL, data1: 'cause', data2: 'control', initialEventId: 'EventId'],

// Informational EventTypes that are not useful for general decision making
(DecisionTaskScheduled)                          : [category: EXTERNAL, state: INFO, initialEventId: 'EventId'],
(DecisionTaskStarted)                            : [category: EXTERNAL, state: INFO, initialEventId: 'EventId'],
(DecisionTaskTimedOut)                           : [category: EXTERNAL, state: INFO, initialEventId: 'EventId'],
(WorkflowExecutionCompleted)                     : [category: EXTERNAL, state: INFO, initialEventId: 'EventId'],
(CompleteWorkflowExecutionFailed)                : [category: EXTERNAL, state: INFO, data1: 'cause', initialEventId: 'EventId'],
(WorkflowExecutionFailed)                        : [category: EXTERNAL, state: INFO, initialEventId: 'EventId'],
(FailWorkflowExecutionFailed)                    : [category: EXTERNAL, state: INFO, initialEventId: 'EventId'],
(WorkflowExecutionTimedOut)                      : [category: EXTERNAL, state: INFO, initialEventId: 'EventId'],
(WorkflowExecutionCanceled)                      : [category: EXTERNAL, state: INFO, initialEventId: 'EventId'],
(CancelWorkflowExecutionFailed)                  : [category: EXTERNAL, state: INFO, initialEventId: 'EventId'],
(WorkflowExecutionContinuedAsNew)                : [category: EXTERNAL, state: INFO, initialEventId: 'EventId'],
(ContinueAsNewWorkflowExecutionFailed)           : [category: EXTERNAL, state: INFO, initialEventId: 'EventId'],
(WorkflowExecutionTerminated)                    : [category: EXTERNAL, state: INFO, initialEventId: 'EventId'],
(ActivityTaskCancelRequested)                    : [category: EXTERNAL, state: INFO, initialEventId: 'EventId'],
(RequestCancelActivityTaskFailed)                : [category: EXTERNAL, state: INFO, initialEventId: 'EventId'],
(RecordMarkerFailed)                             : [category: EXTERNAL, state: INFO, initialEventId: 'EventId'],
(CancelTimerFailed)                              : [category: EXTERNAL, state: INFO, initialEventId: 'EventId'],
(RequestCancelExternalWorkflowExecutionInitiated): [category: EXTERNAL, state: INFO, initialEventId: 'EventId'],
(RequestCancelExternalWorkflowExecutionFailed)   : [category: EXTERNAL, state: INFO, initialEventId: 'EventId'],
(ExternalWorkflowExecutionCancelRequested)       : [category: EXTERNAL, state: INFO, initialEventId: 'EventId']
    ]

}