package com.clario.swift.action;

import com.amazonaws.services.simpleworkflow.model.EventType;
import com.amazonaws.services.simpleworkflow.model.HistoryEvent;
import com.amazonaws.services.simpleworkflow.model.TimerStartedEventAttributes;
import com.clario.swift.EventList;
import com.clario.swift.event.Event;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.amazonaws.services.simpleworkflow.model.EventType.TimerStarted;
import static com.clario.swift.TestUtil.byUpToDecision;
import static com.clario.swift.TestUtil.loadActionEvents;
import static com.clario.swift.action.RetryPolicy.DEFAULT_INITIAL_RETRY_INTERVAL;
import static com.clario.swift.action.RetryPolicyTest.RetryWorkflowStep.*;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.Assert.*;

public class RetryPolicyTest {

    private EventList retryWorkflowEvents = loadActionEvents(RetryPolicy.class, "RetryWorkflowHistory.json");
    private final String control = "RetryPolicy_1412976830265";

    enum RetryWorkflowStep {
        no_decisions,
        decide_activity_initial,
        activity_failed_1,
        retry_timer_fired_1,
        activity_failed_2,
        retry_timer_fired_2,
        activity_failed_3,
        retry_timer_fired_3,
        activity_failed_4,
        retry_timer_fired_4,
        activity_succeeded,
    }

    RetryPolicy retry;

    @Before
    public void before() {
        retry = new RetryPolicy(control);
    }

    @Test(expected = IllegalStateException.class)
    public void testValidateMaxRetryExpirationIntervalLTInitial() {
        new RetryPolicy("retry").withRetryExpirationInterval(SECONDS, 4).validate();
    }

    @Test(expected = IllegalStateException.class)
    public void testValidateMaxRetryIntervalLTInitial() {
        new RetryPolicy("retry").withMaximumRetryInterval(SECONDS, 4).validate();
    }


    @Test
    public void testRetryTerminator() {
        String reason = "WTF ERROR: something wicked this way came";
        String details = "java.lang.IllegalStateException: Failed to invoke with: step1: 1.0 at ....";

        retry.withRetryTerminator(output -> output.matches(".*IllegalStateException.*"));
        assertFalse(retry.testStopRetrying(reason));
        assertTrue(retry.testStopRetrying(details));
        
        retry.withRetryTerminator(output -> output.startsWith("WTF"));
        assertTrue(retry.testStopRetrying(reason));
        assertFalse(retry.testStopRetrying(details));
    }

    @Test
    public void testInitialRetry() {
        retry.validate();
        EventList events = new EventList();
        assertEquals(DEFAULT_INITIAL_RETRY_INTERVAL, retry.nextRetryDelaySeconds(events));
        assertEquals(60, retry.withInitialRetryInterval(MINUTES, 1).nextRetryDelaySeconds(events));
        assertEquals(1, retry.withInitialRetryInterval(SECONDS, 1).nextRetryDelaySeconds(events));
    }

    @Test
    public void testExponentialBackoff() {
        EventList events = selectWorkflowEvents(decide_activity_initial);
        events.selectEventType(TimerStarted);
        assertEquals(0, events.selectRetryCount(control).size());
        assertEquals(5, retry.nextRetryDelaySeconds(events));

        events = selectWorkflowEvents(activity_failed_1);
        assertEquals(0, events.selectRetryCount(control).size());
        assertEquals(5, retry.nextRetryDelaySeconds(events));

        events = selectWorkflowEvents(retry_timer_fired_1);
        assertEquals(1, events.selectRetryCount(control).size());
        assertEquals(10, retry.nextRetryDelaySeconds(events));

        events = selectWorkflowEvents(activity_failed_2);
        assertEquals(1, events.selectRetryCount(control).size());
        assertEquals(10, retry.nextRetryDelaySeconds(events));

        events = selectWorkflowEvents(retry_timer_fired_2);
        assertEquals(2, events.selectRetryCount(control).size());
        assertEquals(20, retry.nextRetryDelaySeconds(events));

        events = selectWorkflowEvents(activity_failed_3);
        assertEquals(2, events.selectRetryCount(control).size());
        assertEquals(20, retry.nextRetryDelaySeconds(events));

        events = selectWorkflowEvents(retry_timer_fired_3);
        assertEquals(3, events.selectRetryCount(control).size());
        assertEquals(40, retry.nextRetryDelaySeconds(events));

        events = selectWorkflowEvents(activity_failed_4);
        assertEquals(3, events.selectRetryCount(control).size());
        assertEquals(40, retry.nextRetryDelaySeconds(events));

        events = selectWorkflowEvents(retry_timer_fired_4);
        assertEquals(4, events.selectRetryCount(control).size());
        assertEquals(80, retry.nextRetryDelaySeconds(events));

        events = selectWorkflowEvents(activity_succeeded);
        assertEquals(4, events.selectRetryCount(control).size());
        assertEquals(80, retry.nextRetryDelaySeconds(events));

        // Play with other retry intervals on the 5th retry
        retry.withInitialRetryInterval(SECONDS, 2);
        assertEquals("Calc 5th backoff with initial of 2 seconds", 32, retry.nextRetryDelaySeconds(events));

        retry.withInitialRetryInterval(SECONDS, 3);
        assertEquals("Calc 5th backoff with initial of 2 seconds", 48, retry.nextRetryDelaySeconds(events));
    }


    @Test
    public void testExponentialBackoffTooManySeconds() {
        int maxSeconds = 10000;
        retry.withMaximumRetryInterval(TimeUnit.SECONDS, maxSeconds);
        List<HistoryEvent> historyEvents = new ArrayList<HistoryEvent>();

        for (int i = 0; i < 30; i++) {
            HistoryEvent he = new HistoryEvent();
            he.setEventType(EventType.TimerStarted);
            he.setTimerStartedEventAttributes(new TimerStartedEventAttributes().withControl(control));
            historyEvents.add(he);
        }
        assertEquals(maxSeconds, retry.nextRetryDelaySeconds(EventList.convert(historyEvents)));
    }


    @Test
    public void testMaximumAttempts() {
        EventList events = selectWorkflowEvents(activity_failed_4);
        assertEquals(3, events.selectRetryCount(control).size());
        assertEquals(40, retry.nextRetryDelaySeconds(events));

        // three retries have occurred so one more is fine
        retry.withMaximumAttempts(4);
        assertEquals(40, retry.nextRetryDelaySeconds(events));
        
        // three retries have occurred so we're finished
        retry.withMaximumAttempts(3);
        assertEquals(-1, retry.nextRetryDelaySeconds(events));

        // shouldn't get here in prod but testing extreme bounds
        retry.withMaximumAttempts(2);
        assertEquals(-1, retry.nextRetryDelaySeconds(events));
    }

    @Test
    public void testRetryExpirationInterval() {
        EventList events = selectWorkflowEvents(retry_timer_fired_3);
        assertEquals(3, events.selectRetryCount(control).size());
        assertEquals(40, retry.nextRetryDelaySeconds(events));

        retry.withRetryExpirationInterval(SECONDS, 62);
        assertEquals("62 is workflow time elapsed + 40 seconds", 40, retry.nextRetryDelaySeconds(events));

        retry.withRetryExpirationInterval(SECONDS, 60);
        assertEquals("Set expiration one second before so retry is timed out", -1, retry.nextRetryDelaySeconds(events));
    }

    @Test
    public void testMaximumRetryInterval() {
        EventList events = selectWorkflowEvents(retry_timer_fired_4);
        assertEquals(4, events.selectRetryCount(control).size());
        assertEquals(80, retry.nextRetryDelaySeconds(events));

        retry.withMaximumRetryInterval(SECONDS, 20);
        assertEquals("expect 20 after limiting retry interval", 20, retry.nextRetryDelaySeconds(events));
    }

    @Test(expected = IllegalStateException.class)
    public void testExceptionIfAssignedToBothFailAndSuccess() {
        Action act = new ActivityAction("mock");
        act.withOnErrorRetryPolicy(retry);
        act.withOnSuccessRetryPolicy(retry);
    }

    private EventList selectWorkflowEvents(RetryWorkflowStep step) {
        return retryWorkflowEvents.select(byUpToDecision(step.ordinal()));
    }

}