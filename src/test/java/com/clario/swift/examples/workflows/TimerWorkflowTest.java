package com.clario.swift.examples.workflows;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.clario.swift.EventList;
import com.clario.swift.TestUtil;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.amazonaws.services.simpleworkflow.model.DecisionType.*;
import static com.clario.swift.TestUtil.byUpToDecision;
import static org.junit.Assert.assertEquals;

public class TimerWorkflowTest {
    private final TimerWorkflow workflow = new TimerWorkflow();
    private EventList eventList;
    private List<Decision> decisions;

    @Before
    public void before() {
        decisions = new ArrayList<Decision>();
        eventList = TestUtil.loadActionEvents(getClass(), "TimerWorkflowHistory.json");
        workflow.reset();
    }

    @Test
    public void testBeforeTimerAction() {
        workflow.addEvents(eventList.select(byUpToDecision(1)));
        workflow.decide(decisions);
        Assert.assertEquals("100", workflow.getWorkflowInput());
        assertEquals(1, decisions.size());
        Decision decision = decisions.get(0);
        TestUtil.assertEquals(decision, ScheduleActivityTask);
        assertEquals(workflow.beforeTimer.getActionId(), decision.getScheduleActivityTaskDecisionAttributes().getActivityId());
    }

    @Test
    public void testTimerAction() {
        workflow.addEvents(eventList.select(byUpToDecision(2)));
        workflow.decide(decisions);
        Assert.assertEquals("100", workflow.getWorkflowInput());
        assertEquals(1, decisions.size());
        Decision decision = decisions.get(0);
        TestUtil.assertEquals(decision, StartTimer);
        assertEquals(workflow.timerAction.getActionId(), decision.getStartTimerDecisionAttributes().getTimerId());
        assertEquals(workflow.timerAction.getControl(), decision.getStartTimerDecisionAttributes().getControl());
        assertEquals(workflow.timerAction.getStartToFireTimeout(), decision.getStartTimerDecisionAttributes().getStartToFireTimeout());

    }

    @Test
    public void testAfterTimerAction() {
        workflow.addEvents(eventList.select(byUpToDecision(3)));
        workflow.decide(decisions);
        Assert.assertEquals("100", workflow.getWorkflowInput());
        assertEquals(1, decisions.size());
        Decision decision = decisions.get(0);
        TestUtil.assertEquals(decision, ScheduleActivityTask);
        assertEquals(workflow.afterTimer.getActionId(), decision.getScheduleActivityTaskDecisionAttributes().getActivityId());
    }

    @Test
    public void testWorkflowComplete() {
        workflow.addEvents(eventList.select(byUpToDecision(4)));
        workflow.decide(decisions);
        Assert.assertEquals("100", workflow.getWorkflowInput());
        assertEquals(1, decisions.size());
        Decision decision = decisions.get(0);
        TestUtil.assertEquals(decision, CompleteWorkflowExecution);
        assertEquals("201", decision.getCompleteWorkflowExecutionDecisionAttributes().getResult());
    }
}