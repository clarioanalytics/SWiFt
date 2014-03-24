package com.clario.swift

import com.amazonaws.services.simpleworkflow.model.*
import com.amazonaws.services.simpleworkflow.model.transform.DecisionTaskJsonUnmarshaller
import com.amazonaws.services.simpleworkflow.model.transform.HistoryEventJsonUnmarshaller
import com.amazonaws.transform.JsonUnmarshallerContext
import com.amazonaws.transform.ListUnmarshaller
import com.amazonaws.transform.Unmarshaller
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonParser
import spock.lang.Specification

import static com.amazonaws.services.simpleworkflow.model.EventType.*

/**
 * @author George Coller
 */
public class HistoryInspectorTest extends Specification {

    static final DecisionTask unmarshalDecisionTask(String json) {
        Unmarshaller<DecisionTask, JsonUnmarshallerContext> unmarshaller = new DecisionTaskJsonUnmarshaller()
        JsonParser parser = new JsonFactory().createParser(json)
        unmarshaller.unmarshall(new JsonUnmarshallerContext(parser))
    }

    static final List<HistoryEvent> unmarshalHistoryEvents(String json) {
        JsonParser parser = new JsonFactory().createParser(json)
        new ListUnmarshaller<HistoryEvent>(HistoryEventJsonUnmarshaller.getInstance()).unmarshall(new JsonUnmarshallerContext(parser))
    }

    def 'event attributes for history event'() {
        when:
        def he = new HistoryEvent()
        he.eventId = 101
        he.activityTaskScheduledEventAttributes = new ActivityTaskScheduledEventAttributes().withActivityId("UID")
        he.activityTaskStartedEventAttributes = new ActivityTaskStartedEventAttributes(scheduledEventId: ActivityTaskStarted.hashCode())
        he.activityTaskCompletedEventAttributes = new ActivityTaskCompletedEventAttributes(scheduledEventId: ActivityTaskCompleted.hashCode())
        he.activityTaskFailedEventAttributes = new ActivityTaskFailedEventAttributes(scheduledEventId: ActivityTaskFailed.hashCode())
        he.activityTaskTimedOutEventAttributes = new ActivityTaskTimedOutEventAttributes(scheduledEventId: ActivityTaskTimedOut.hashCode())
        he.activityTaskCanceledEventAttributes = new ActivityTaskCanceledEventAttributes(scheduledEventId: ActivityTaskCanceled.hashCode())

        he.timerStartedEventAttributes = new TimerStartedEventAttributes().withTimerId("UID")
        he.timerFiredEventAttributes = new TimerFiredEventAttributes(startedEventId: TimerFired.hashCode())
        he.timerCanceledEventAttributes = new TimerCanceledEventAttributes(startedEventId: TimerCanceled.hashCode())

        he.startChildWorkflowExecutionInitiatedEventAttributes = new StartChildWorkflowExecutionInitiatedEventAttributes().withWorkflowId("UID")
        he.childWorkflowExecutionStartedEventAttributes = new ChildWorkflowExecutionStartedEventAttributes(initiatedEventId: ChildWorkflowExecutionStarted.hashCode())
        he.childWorkflowExecutionCompletedEventAttributes = new ChildWorkflowExecutionCompletedEventAttributes(initiatedEventId: ChildWorkflowExecutionCompleted.hashCode())
        he.childWorkflowExecutionFailedEventAttributes = new ChildWorkflowExecutionFailedEventAttributes(initiatedEventId: ChildWorkflowExecutionFailed.hashCode())
        he.childWorkflowExecutionTimedOutEventAttributes = new ChildWorkflowExecutionTimedOutEventAttributes(initiatedEventId: ChildWorkflowExecutionTimedOut.hashCode())
        he.childWorkflowExecutionCanceledEventAttributes = new ChildWorkflowExecutionCanceledEventAttributes(initiatedEventId: ChildWorkflowExecutionCanceled.hashCode())
        he.childWorkflowExecutionTerminatedEventAttributes = new ChildWorkflowExecutionTerminatedEventAttributes(initiatedEventId: ChildWorkflowExecutionTerminated.hashCode())

        def table = [
                [ActivityTaskScheduled, he.activityTaskScheduledEventAttributes, 101],
                [ActivityTaskStarted, he.activityTaskStartedEventAttributes],
                [ActivityTaskCompleted, he.activityTaskCompletedEventAttributes],
                [ActivityTaskFailed, he.activityTaskFailedEventAttributes],
                [ActivityTaskTimedOut, he.activityTaskTimedOutEventAttributes],
                [ActivityTaskCanceled, he.activityTaskCanceledEventAttributes],

                [TimerStarted, he.timerStartedEventAttributes, 101],
                [TimerFired, he.timerFiredEventAttributes],
                [TimerCanceled, he.timerCanceledEventAttributes],

                [StartChildWorkflowExecutionInitiated, he.startChildWorkflowExecutionInitiatedEventAttributes, 101],
                [ChildWorkflowExecutionStarted, he.childWorkflowExecutionStartedEventAttributes],
                [ChildWorkflowExecutionCompleted, he.childWorkflowExecutionCompletedEventAttributes],
                [ChildWorkflowExecutionFailed, he.childWorkflowExecutionFailedEventAttributes],
                [ChildWorkflowExecutionTimedOut, he.childWorkflowExecutionTimedOutEventAttributes],
                [ChildWorkflowExecutionCanceled, he.childWorkflowExecutionCanceledEventAttributes],
                [ChildWorkflowExecutionTerminated, he.childWorkflowExecutionTerminatedEventAttributes],
        ]
        then:
        table.each { row ->
            he.eventType = row[0]
            StepEvent step = new StepEvent(he)
            assert step.attributes != null && step.attributes == row[1]
            if (step.isInitialStepEvent()) {
                assert step.initialStepEventId == 101
                assert step.stepId == "UID"
            } else {
                assert step.initialStepEventId == row[0].hashCode()
            }
        }
    }
}