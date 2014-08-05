package com.clario.swift.examples.workflows;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.clario.swift.MockWorkflowHistory;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.amazonaws.services.simpleworkflow.model.DecisionType.*;
import static org.junit.Assert.assertEquals;

public class TimerWorkflowTest {
    private final TimerWorkflow workflow = new TimerWorkflow();
    private final MockWorkflowHistory mock = new MockWorkflowHistory();
    private List<Decision> decisions;

    @Before
    public void before() {
        decisions = new ArrayList<Decision>();
        mock.loadEvents(getClass(), "TimerWorkflowHistory.json");
    }

    @Test
    public void testBeforeTimerAction() {
        workflow.addHistoryEvents(mock.withStopAtDecisionTaskStarted(1).getEvents());
        workflow.decide(decisions);
        Assert.assertEquals("100", workflow.getWorkflowInput());
        assertEquals(1, decisions.size());
        Decision decision = decisions.get(0);
        MockWorkflowHistory.assertEquals(decision, ScheduleActivityTask);
        assertEquals(workflow.beforeTimer.getActionId(), decision.getScheduleActivityTaskDecisionAttributes().getActivityId());
    }

    @Test
    public void testTimerAction() {
        workflow.addHistoryEvents(mock.withStopAtDecisionTaskStarted(2).getEvents());
        workflow.decide(decisions);
        Assert.assertEquals("100", workflow.getWorkflowInput());
        assertEquals(1, decisions.size());
        Decision decision = decisions.get(0);
        MockWorkflowHistory.assertEquals(decision, StartTimer);
        assertEquals(workflow.timerAction.getActionId(), decision.getStartTimerDecisionAttributes().getTimerId());
        assertEquals(workflow.timerAction.getControl(), decision.getStartTimerDecisionAttributes().getControl());
        assertEquals(workflow.timerAction.getStartToFireTimeout(), decision.getStartTimerDecisionAttributes().getStartToFireTimeout());

    }

    @Test
    public void testAfterTimerAction() {
        workflow.addHistoryEvents(mock.withStopAtDecisionTaskStarted(3).getEvents());
        workflow.decide(decisions);
        Assert.assertEquals("100", workflow.getWorkflowInput());
        assertEquals(1, decisions.size());
        Decision decision = decisions.get(0);
        MockWorkflowHistory.assertEquals(decision, ScheduleActivityTask);
        assertEquals(workflow.afterTimer.getActionId(), decision.getScheduleActivityTaskDecisionAttributes().getActivityId());
    }

    @Test
    public void testWorkflowComplete() {
        workflow.addHistoryEvents(mock.withStopAtDecisionTaskStarted(4).getEvents());
        workflow.decide(decisions);
        Assert.assertEquals("100", workflow.getWorkflowInput());
        assertEquals(1, decisions.size());
        Decision decision = decisions.get(0);
        MockWorkflowHistory.assertEquals(decision, CompleteWorkflowExecution);
        assertEquals("201", decision.getCompleteWorkflowExecutionDecisionAttributes().getResult());
    }
}