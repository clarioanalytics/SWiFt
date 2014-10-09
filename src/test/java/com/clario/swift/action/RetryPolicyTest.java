package com.clario.swift.action;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.clario.swift.EventList;
import com.clario.swift.TestUtil;
import com.clario.swift.Workflow;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.clario.swift.TestUtil.byUpToDecision;
import static com.clario.swift.TestUtil.loadActionEvents;
import static com.clario.swift.action.RetryPolicy.DEFAULT_INITIAL_RETRY_INTERVAL;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.*;

public class RetryPolicyTest {
    @Test(expected = IllegalStateException.class)
    public void testValidateMaxRetryExpirationIntervalLTInitial() {
        new RetryPolicy().withRetryExpirationInterval(SECONDS, 4).validate();
    }

    @Test(expected = IllegalStateException.class)
    public void testValidateMaxRetryIntervalLTInitial() {
        new RetryPolicy().withMaximumRetryInterval(SECONDS, 4).validate();
    }

    Workflow workflow = TestUtil.MOCK_WORKFLOW;
    Action action = new ActivityAction("step1");
    RetryPolicy retry = new RetryPolicy();

    @Before
    public void before() {
        workflow.reset();
        action.setWorkflow(workflow);
        action.withRetryPolicy(retry);
    }

    @Test
    public void testMatchesRegEx() {
        workflow.addEvents(loadActionEvents(RetryPolicy.class, "ActivityFailedWorkflowHistory.json"));
        List<Decision> decisions = new ArrayList<Decision>();

        retry.withStopIfErrorMatches(".*IllegalStateException.*");
        assertFalse(retry.decide(action, decisions));

        retry.withStopIfErrorMatches(".*step\\d.*");
        assertFalse(retry.decide(action, decisions));

        retry.withStopIfErrorMatches(".*w.cked.*came.*");
        assertFalse(retry.decide(action, decisions));

        retry.withStopIfErrorMatches("^WTF.*");
        assertFalse(retry.decide(action, decisions));

        assertEquals(0, decisions.size());

        retry.withStopIfErrorMatches("IllegalStateException");
        assertTrue(retry.decide(action, decisions));
        retry.withStopIfErrorMatches("XYZ");
        assertTrue(retry.decide(action, decisions));
        assertEquals(2, decisions.size());
    }

    @Test
    public void testInitialRetry() {
        retry.validate();
        assertEquals(0, action.getRetryCount());
        assertEquals(DEFAULT_INITIAL_RETRY_INTERVAL, retry.nextRetryDelaySeconds(action));
        assertEquals(60, retry.withInitialRetryInterval(MINUTES, 1).nextRetryDelaySeconds(action));
        assertEquals(1, retry.withInitialRetryInterval(SECONDS, 1).nextRetryDelaySeconds(action));
    }

    @Test
    public void testRetryExpirationInterval() {
        EventList events = loadActionEvents(RetryPolicy.class, "RetryWorkflowHistory.json");

        // workflow started, decide activity
        workflow.replaceEvents(events.select(byUpToDecision(1)));
        assertEquals(0, action.getRetryCount());

        // first time activity failed, decide retry timer
        workflow.replaceEvents(events.select(byUpToDecision(2)));
        assertEquals(0, action.getRetryCount());
        assertEquals(5, retry.nextRetryDelaySeconds(action));

        // timer fired, decide retry activity
        workflow.replaceEvents(events.select(byUpToDecision(3)));
        assertEquals(1, action.getRetryCount());
        assertEquals(10, retry.nextRetryDelaySeconds(action));

        // second time activity failed, decide retry timer
        workflow.replaceEvents(events.select(byUpToDecision(4)));
        assertEquals(1, action.getRetryCount());
        assertEquals(10, retry.nextRetryDelaySeconds(action));


        // timer fired, decide retry activity
        workflow.replaceEvents(events.select(byUpToDecision(5)));
        assertEquals(2, action.getRetryCount());
        assertEquals(20, retry.nextRetryDelaySeconds(action));

        // third time activity failed, decide retry timer
        workflow.replaceEvents(events.select(byUpToDecision(6)));
        assertEquals(2, action.getRetryCount());
        assertEquals(20, retry.nextRetryDelaySeconds(action));

        // timer fired, decide retry activity
        workflow.replaceEvents(events.select(byUpToDecision(7)));
        assertEquals(3, action.getRetryCount());
        assertEquals(40, retry.nextRetryDelaySeconds(action));

        // third time activity failed, decide retry timer
        workflow.replaceEvents(events.select(byUpToDecision(8)));
        assertEquals(3, action.getRetryCount());
        assertEquals(40, retry.nextRetryDelaySeconds(action));


        // timer fired, decide retry activity
        workflow.replaceEvents(events.select(byUpToDecision(9)));
        assertEquals(4, action.getRetryCount());
        assertEquals(80, retry.nextRetryDelaySeconds(action));

        // fourth time activity succeeded! decide workflow complete
        workflow.replaceEvents(events.select(byUpToDecision(10)));
        assertEquals(4, action.getRetryCount());
        assertEquals(80, retry.nextRetryDelaySeconds(action));


        // Play with other settings on the 5th retry
        retry.withInitialRetryInterval(SECONDS, 2);
        assertEquals(32, retry.nextRetryDelaySeconds(action));

        retry.withInitialRetryInterval(SECONDS, 3);
        assertEquals(48, retry.nextRetryDelaySeconds(action));

        retry.withRetryExpirationInterval(SECONDS, 91);
        assertEquals("91 is workflow time elapsed + 48 seconds", 48, retry.nextRetryDelaySeconds(action));

        retry.withRetryExpirationInterval(SECONDS, 90);
        assertEquals("Set expiration one second before so retry is timed out", -1, retry.nextRetryDelaySeconds(action));
    }

//    @Test
//    public void testMaximumRetryInterval() {
//        RetryPolicy retry = new RetryPolicy();
//        Action action = new ActivityAction("step1");
//        retry.withInitialRetryInterval(SECONDS, 5);
//        DateTime dateTime = new DateTime(2000, 1, 1, 12, 0, 0);
//        action.currentHistoryEvent = makeTimerFired(dateTime.minusSeconds(1), 100);
//        action.events.add(createTimerStarted(dateTime.minusSeconds(10), 100));
//        action.events.add(createTimerStarted(dateTime.minusSeconds(20), 90));
//        assertEquals(2, action.getRetryCount());
//        assertEquals(10, retry.nextRetryDelaySeconds(action));
//        retry.withMaximumRetryInterval(SECONDS, 9);
//        assertEquals(9, retry.nextRetryDelaySeconds(action));
//    }
//
//    @Test
//    public void testIsRetryTimerEvent() {
//        RetryPolicy retry = new RetryPolicy();
//        Action action = new ActivityAction("step1");
//        action.withRetryPolicy(retry);
//        retry.withInitialRetryInterval(SECONDS, 2);
//        retry.withRetryExpirationInterval(SECONDS, 60);
//        DateTime dateTime = new DateTime(2000, 1, 1, 12, 0, 0);
//        assertEquals(0, action.getRetryCount());
//
//        action.currentHistoryEvent = makeTimerFired(dateTime.minusSeconds(1), 100);
//        action.events.add(createTimerStarted(dateTime.minusSeconds(10), 100));
//        assertEquals(1, action.getRetryCount());
//        assertEquals(2, retry.nextRetryDelaySeconds(action));
//
//        action.events.add(createTimerStarted(dateTime.minusSeconds(20), 90));
//        assertEquals(2, action.getRetryCount());
//        assertEquals(4, retry.nextRetryDelaySeconds(action));
//
//        action.events.add(createTimerStarted(dateTime.minusSeconds(30), 80));
//        assertEquals(3, action.getRetryCount());
//        assertEquals(8, retry.nextRetryDelaySeconds(action));
//
//        action.events.add(createTimerStarted(dateTime.minusSeconds(40), 80));
//        assertEquals(4, action.getRetryCount());
//        assertEquals(16, retry.nextRetryDelaySeconds(action));
//
//        //
//        retry.withMaximumRetryInterval(SECONDS, 10);
//        assertEquals(10, retry.nextRetryDelaySeconds(action));
//    }
}