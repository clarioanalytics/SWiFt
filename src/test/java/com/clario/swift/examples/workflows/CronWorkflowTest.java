package com.clario.swift.examples.workflows;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.amazonaws.services.simpleworkflow.model.DecisionType;
import com.clario.swift.MockWorkflowHistory;
import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.amazonaws.services.simpleworkflow.model.DecisionType.ScheduleActivityTask;
import static com.amazonaws.services.simpleworkflow.model.DecisionType.StartTimer;
import static org.junit.Assert.assertEquals;

/**
 * @author George Coller
 */
public class CronWorkflowTest {

    private final CronWorkflow workflow = new CronWorkflow();
    private final MockWorkflowHistory mock = new MockWorkflowHistory();
    private List<Decision> decisions;

    @Before
    public void before() {
        decisions = new ArrayList<Decision>();
        mock.loadEvents(getClass(), "CronWorkflowHistory.json");
        workflow.getWorkflowHistory().reset();
    }

    @Test
    public void testWorkflowInputAndStartDate() {
        Date wfStartTime = setWorkflowStartTimeNowMinus(1);
        workflow.addHistoryEvents(mock.withStopAtDecisionTaskStarted(1).getEvents());
        assertEquals("6", workflow.getWorkflowInput());
        assertEquals(wfStartTime, workflow.getWorkflowHistory().getWorkflowStartDate());
    }

    @Test
    public void testContinueAsNew() {
        setWorkflowStartTimeNowMinus(60);
        workflow.addHistoryEvents(mock.withStopAtDecisionTaskStarted(13).getEvents());
        workflow.decide(decisions);
        assertEquals(1, decisions.size());
        Decision decision = decisions.get(0);
        MockWorkflowHistory.assertEquals(decision, DecisionType.ContinueAsNewWorkflowExecution);
    }

    @Test
    public void testCronStepAlternationBetweenActivityAndTimer() {
        setWorkflowStartTimeNowMinus(59);

        List<Integer> steps = Arrays.asList(1, 3, 5, 7, 9, 11);
        int startCountAt = 6;
        for (Integer step : steps) {
            workflow.getWorkflowHistory().reset();
            workflow.addHistoryEvents(mock.withStopAtDecisionTaskStarted(step).getEvents());
            assertCurrentCount(String.valueOf(startCountAt));

            step++;
            workflow.getWorkflowHistory().reset();
            workflow.addHistoryEvents(mock.withStopAtDecisionTaskStarted(step).getEvents());
            assertTimerDecision();
            startCountAt++;
        }
    }

    private Date setWorkflowStartTimeNowMinus(int seconds) {
        Date timestamp = DateTime.now().minusSeconds(seconds).toDate();
        mock.resetTimestampsStartingAt(timestamp);
        return timestamp;
    }

    private void assertTimerDecision() {
        decisions.clear();
        workflow.decide(decisions);
        assertEquals(1, decisions.size());
        Decision decision = decisions.get(0);
        MockWorkflowHistory.assertEquals(decision, StartTimer);
        assertEquals("10", decision.getStartTimerDecisionAttributes().getStartToFireTimeout());
    }

    private void assertCurrentCount(String currentCount) {
        decisions.clear();
        workflow.decide(decisions);
        assertEquals(1, decisions.size());
        Decision decision = decisions.get(0);
        MockWorkflowHistory.assertEquals(decision, ScheduleActivityTask);
        assertEquals(workflow.echoActivity.getActionId(), decision.getScheduleActivityTaskDecisionAttributes().getActivityId());
        assertEquals(currentCount, decision.getScheduleActivityTaskDecisionAttributes().getInput());
    }

}
