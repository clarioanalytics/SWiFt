package com.clario.swift.examples.workflows;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.amazonaws.services.simpleworkflow.model.DecisionType;
import com.clario.swift.EventList;
import com.clario.swift.TestUtil;
import com.clario.swift.action.ActivityAction;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.amazonaws.services.simpleworkflow.model.DecisionType.*;
import static com.clario.swift.TestUtil.assertActionCausedWorkflowExecutionDecision;
import static com.clario.swift.TestUtil.byUpToDecision;
import static org.junit.Assert.assertEquals;

/**
 * Example of unit testing a workflow using mocked workflow events.
 *
 * @author George Coller
 */
public class SimpleWorkflowTest {

    private final SimpleWorkflow workflow = new SimpleWorkflow();
    private EventList eventList = TestUtil.loadActionEvents(getClass(), "SimpleWorkflowHistory.json");
    private List<Decision> decisions;

    @Before
    public void before() {
        decisions = new ArrayList<>();
    }

    private static void assertDecisionType(Decision decision, DecisionType decisionType) {
        assertEquals(decision.getDecisionType(), decisionType.toString());
    }

    @Test
    public void testScheduleStep1() {
        workflow.replaceEvents(eventList.select(byUpToDecision(1)));
        assertEquals("100", workflow.getWorkflowInput());
        workflow.decide(decisions);
        assertEquals(1, decisions.size());
        Decision decision = decisions.get(0);
        assertDecisionType(decision, ScheduleActivityTask);
        assertEquals(workflow.step1.getActionId(), decision.getScheduleActivityTaskDecisionAttributes().getActivityId());
    }

    @Test
    public void testScheduleStep2() {
        workflow.replaceEvents(eventList.select(byUpToDecision(2)));
        workflow.decide(decisions);
        assertEquals(1, decisions.size());
        Decision decision = decisions.get(0);
        assertDecisionType(decision, ScheduleActivityTask);
        assertEquals(workflow.step2.getActionId(), decision.getScheduleActivityTaskDecisionAttributes().getActivityId());
    }

    @Test
    public void testScheduleStep3() {
        workflow.replaceEvents(eventList.select(byUpToDecision(3)));
        workflow.decide(decisions);
        assertEquals(1, decisions.size());
        Decision decision = decisions.get(0);
        assertDecisionType(decision, ScheduleActivityTask);
        assertEquals(workflow.step3.getActionId(), decision.getScheduleActivityTaskDecisionAttributes().getActivityId());
    }

    @Test
    public void testComplete() {
        workflow.replaceEvents(eventList.select(byUpToDecision(4)));
        workflow.decide(decisions);
        assertEquals(1, decisions.size());
        Decision decision = decisions.get(0);
        assertDecisionType(decision, CompleteWorkflowExecution);
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
        eventList = eventList.select(byUpToDecision(stopAtDecisionTask));
        eventList = TestUtil.convertActivitySuccessToFail(eventList, null, "mock reason", "mock detail");
        workflow.replaceEvents(eventList);

        workflow.decide(decisions);
        assertEquals(1, decisions.size());
        Decision decision = decisions.get(0);
        assertDecisionType(decision, FailWorkflowExecution);
        assertActionCausedWorkflowExecutionDecision(action, decision, "mock reason", "mock detail");
    }
}
