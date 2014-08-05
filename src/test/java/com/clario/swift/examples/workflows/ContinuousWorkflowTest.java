package com.clario.swift.examples.workflows;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.clario.swift.MockWorkflowHistory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.amazonaws.services.simpleworkflow.model.DecisionType.ContinueAsNewWorkflowExecution;
import static org.junit.Assert.assertEquals;

public class ContinuousWorkflowTest {
    private final ContinuousWorkflow workflow = new ContinuousWorkflow();
    private final MockWorkflowHistory mock = new MockWorkflowHistory();
    private List<Decision> decisions;

    @Before
    public void before() {
        decisions = new ArrayList<Decision>();
        mock.loadEvents(getClass(), "ContinuousWorkflowHistory.json");
    }

    @Test
    public void testDecisionTask1() {
        workflow.addHistoryEvents(mock.withStopAtDecisionTaskStarted(1).getEvents());
        workflow.decide(decisions);
        assertEquals(0, decisions.size());
    }

    @Test
    public void testDecisionTask2() {
        workflow.addHistoryEvents(mock.withStopAtDecisionTaskStarted(2).getEvents());
        workflow.decide(decisions);
        assertEquals(1, decisions.size());
        Decision decision = decisions.get(0);
        MockWorkflowHistory.assertEquals(decision, ContinueAsNewWorkflowExecution);
        Assert.assertEquals("Mock Signal Input", decision.getContinueAsNewWorkflowExecutionDecisionAttributes().getInput());
    }
}