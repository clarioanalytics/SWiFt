package com.clario.swift.examples.workflows;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.clario.swift.EventList;
import com.clario.swift.TestUtil;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.amazonaws.services.simpleworkflow.model.DecisionType.ScheduleActivityTask;
import static com.amazonaws.services.simpleworkflow.model.DecisionType.StartTimer;
import static org.junit.Assert.assertEquals;

/**
 * @author George Coller
 */
public class CronWorkflowTest {

    private final CronWorkflow workflow = new CronWorkflow();
    private EventList mock;
    private List<Decision> decisions;

    @Before
    public void before() {
        decisions = new ArrayList<Decision>();
        mock = TestUtil.loadActionEvents(getClass(), "CronWorkflowHistory.json");
        workflow.reset();
    }

    @Test
    public void testWorkflowInputAndStartDate() {
//        EventList events = mock.select(MockEventList.byBeforeDecisionTaskCompleted(1)).resetTimestampsStartingAt(1);
//        events = TestUtil.resetTimestampsStartingAt(events, );
//        Event last = events.getLast();
//        List<Event> historyEvents = events;
//        workflow.addEvents(historyEvents);
//        assertEquals("6", workflow.getWorkflowInput());
//        assertEquals(last.getEventTimestamp().toDate(), workflow.getWorkflowStartDate());
    }

    @Test
    public void testContinueAsNew() {
//        workflow.addEvents(mock.select(byBeforeDecisionTaskCompleted(13))
//                .resetTimestampsStartingAt(59)
//        );
//        workflow.decide(decisions);
//        assertEquals(1, decisions.size());
//        Decision decision = decisions.get(0);
//        TestUtil.assertEquals(decision, DecisionType.ContinueAsNewWorkflowExecution);
    }

    @Test
    public void testCronStepAlternationBetweenActivityAndTimer() {
//        mock.resetTimestampsStartingAt(59);
//        List<Integer> steps = Arrays.asList(1, 3, 5, 7, 9, 11);
//        int startCountAt = 6;
//        for (Integer step : steps) {
//            workflow.reset();
//            workflow.addEvents(mock.byBeforeDecisionTaskCompleted(step));
//            assertCurrentCount(String.valueOf(startCountAt));
//
//            step++;
//            workflow.reset();
//            workflow.addEvents(mock.byBeforeDecisionTaskCompleted(step));
//            assertTimerDecision();
//            startCountAt++;
//        }
    }

    private void assertTimerDecision() {
        decisions.clear();
        workflow.decide(decisions);
        assertEquals(1, decisions.size());
        Decision decision = decisions.get(0);
        TestUtil.assertEquals(decision, StartTimer);
        assertEquals("10", decision.getStartTimerDecisionAttributes().getStartToFireTimeout());
    }

    private void assertCurrentCount(String currentCount) {
        decisions.clear();
        workflow.decide(decisions);
        assertEquals(1, decisions.size());
        Decision decision = decisions.get(0);
        TestUtil.assertEquals(decision, ScheduleActivityTask);
        assertEquals(workflow.echoActivity.getActionId(), decision.getScheduleActivityTaskDecisionAttributes().getActivityId());
        assertEquals(currentCount, decision.getScheduleActivityTaskDecisionAttributes().getInput());
    }

}
