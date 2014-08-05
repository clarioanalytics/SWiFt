package com.clario.swift.examples.workflows;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.clario.swift.MockWorkflowHistory;
import com.clario.swift.action.ActivityAction;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.amazonaws.services.simpleworkflow.model.DecisionType.CompleteWorkflowExecution;
import static com.amazonaws.services.simpleworkflow.model.DecisionType.ScheduleActivityTask;
import static com.clario.swift.MockWorkflowHistory.assertActionCausedWorkflowExecutionDecision;
import static com.clario.swift.MockWorkflowHistory.assertEquals;
import static org.junit.Assert.assertEquals;

/**
 * Example of unit testing a workflow using mocked history events.
 *
 * @author George Coller
 */
public class SimpleWorkflowTest {

    private final SimpleWorkflow workflow = new SimpleWorkflow();
    private final MockWorkflowHistory mock = new MockWorkflowHistory();
    private List<Decision> decisions;

    @Before
    public void before() {
        decisions = new ArrayList<Decision>();
        mock.loadEvents(getClass(), "SimpleWorkflowHistory.json");
    }

    @Test
    public void testScheduleStep1() {
        workflow.addHistoryEvents(mock.withStopAtDecisionTaskStarted(1).getEvents());
        assertEquals("100", workflow.getWorkflowInput());
        workflow.decide(decisions);
        assertEquals(1, decisions.size());
        Decision decision = decisions.get(0);
        assertEquals(decision, ScheduleActivityTask);
        assertEquals(workflow.step1.getActionId(), decision.getScheduleActivityTaskDecisionAttributes().getActivityId());
    }

    @Test
    public void testScheduleStep2() {
        workflow.addHistoryEvents(mock.withStopAtDecisionTaskStarted(2).getEvents());
        workflow.decide(decisions);
        assertEquals(1, decisions.size());
        Decision decision = decisions.get(0);
        assertEquals(decision, ScheduleActivityTask);
        assertEquals(workflow.step2.getActionId(), decision.getScheduleActivityTaskDecisionAttributes().getActivityId());
    }

    @Test
    public void testScheduleStep3() {
        workflow.addHistoryEvents(mock.withStopAtDecisionTaskStarted(3).getEvents());
        workflow.decide(decisions);
        assertEquals(1, decisions.size());
        Decision decision = decisions.get(0);
        assertEquals(decision, ScheduleActivityTask);
        assertEquals(workflow.step3.getActionId(), decision.getScheduleActivityTaskDecisionAttributes().getActivityId());
    }

    @Test
    public void testComplete() {
        workflow.addHistoryEvents(mock.withStopAtDecisionTaskStarted(4).getEvents());
        workflow.decide(decisions);
        assertEquals(1, decisions.size());
        Decision decision = decisions.get(0);
        assertEquals(decision, CompleteWorkflowExecution);
    }

    @Test
    public void testStep1Failed() {
        assertFailedStep(workflow.step1, 2);
    }

    @Test
    public void testStep2Failed() {
        assertFailedStep(workflow.step2, 3);
    }

    @Test
    public void testStep3Failed() {
        assertFailedStep(workflow.step3, 4);
    }

    protected void assertFailedStep(ActivityAction action, int stopAtDecisionTask) {
        workflow.addHistoryEvents(mock
            .withStopAtDecisionTaskStarted(stopAtDecisionTask)
            .withConvertLastActivityTaskToFailed("mock reason", "mock detail").getEvents());

        workflow.decide(decisions);
        assertEquals(1, decisions.size());
        Decision decision = decisions.get(0);
        assertActionCausedWorkflowExecutionDecision(action, decision, "mock reason", "mock detail");
    }

}
