package com.clario.swift.action;

import com.amazonaws.services.simpleworkflow.model.*;
import com.clario.swift.ActionEvent;
import org.joda.time.DateTime;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.amazonaws.services.simpleworkflow.model.EventType.ActivityTaskFailed;
import static com.clario.swift.action.RetryPolicy.DEFAULT_INITIAL_RETRY_INTERVAL;
import static java.lang.Integer.MAX_VALUE;
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

    @Test
    public void testMatchesRegEx() {
        MockRetry retry = createMockRetry();
        HistoryEvent he = new HistoryEvent();
        he.setEventType(ActivityTaskFailed);
        he.setActivityTaskFailedEventAttributes(new ActivityTaskFailedEventAttributes()
            .withScheduledEventId(123L)
            .withReason("WTF ERROR: something wicked this way came")
            .withDetails("java.lang.IllegalStateException: Failed to invoke with: step1: 1.0 at ...."));
        retry.currentHistoryEvent = new ActionEvent(he);

        List<Decision> decisions = new ArrayList<>();

        retry.withStopIfErrorMatches(".*IllegalStateException.*");
        assertFalse(retry.decide(decisions));

        retry.withStopIfErrorMatches(".*step\\d.*");
        assertFalse(retry.decide(decisions));

        retry.withStopIfErrorMatches(".*w.cked.*came.*");
        assertFalse(retry.decide(decisions));

        retry.withStopIfErrorMatches("^WTF.*");
        assertFalse(retry.decide(decisions));

        assertEquals(0, decisions.size());

        retry.withStopIfErrorMatches("IllegalStateException");
        assertTrue(retry.decide(decisions));
        retry.withStopIfErrorMatches("XYZ");
        assertTrue(retry.decide(decisions));
        assertEquals(2, decisions.size());
    }

    @Test
    public void testInitialRetry() {
        MockRetry retry = createMockRetry();
        retry.validate();
        assertEquals(0, retry.getRetryCount());
        assertEquals(DEFAULT_INITIAL_RETRY_INTERVAL, retry.nextRetryDelaySeconds());
        assertEquals(60, retry.withInitialRetryInterval(MINUTES, 1).nextRetryDelaySeconds());
        assertEquals(1, retry.withInitialRetryInterval(SECONDS, 1).nextRetryDelaySeconds());
        assertEquals(MAX_VALUE, retry.withInitialRetryInterval(SECONDS, 0).nextRetryDelaySeconds());
        assertEquals(MAX_VALUE, retry.withInitialRetryInterval(SECONDS, -1).nextRetryDelaySeconds());
    }

    @Test
    public void testRetryExpirationInterval() {
        MockRetry retry = createMockRetry();
        retry.withInitialRetryInterval(SECONDS, 5);
        DateTime dateTime = new DateTime(2000, 1, 1, 12, 0, 0);
        retry.currentHistoryEvent = makeTimerFired(dateTime, 100);
        retry.events.add(createTimerStarted(dateTime.minusSeconds(10), 100));
        retry.events.add(createTimerStarted(dateTime.minusSeconds(20), 90));
        assertEquals(2, retry.getRetryCount());
        assertEquals(10, retry.nextRetryDelaySeconds());

        retry.withRetryExpirationInterval(SECONDS, 30);
        assertEquals(10, retry.nextRetryDelaySeconds());

        retry.withRetryExpirationInterval(SECONDS, 29);
        assertEquals(-1, retry.nextRetryDelaySeconds());
    }

    @Test
    public void testMaximumRetryInterval() {
        MockRetry retry = createMockRetry();
        retry.withInitialRetryInterval(SECONDS, 5);
        DateTime dateTime = new DateTime(2000, 1, 1, 12, 0, 0);
        retry.currentHistoryEvent = makeTimerFired(dateTime.minusSeconds(1), 100);
        retry.events.add(createTimerStarted(dateTime.minusSeconds(10), 100));
        retry.events.add(createTimerStarted(dateTime.minusSeconds(20), 90));
        assertEquals(2, retry.getRetryCount());
        assertEquals(10, retry.nextRetryDelaySeconds());
        retry.withMaximumRetryInterval(SECONDS, 9);
        assertEquals(9, retry.nextRetryDelaySeconds());
    }

    @Test
    public void testIsRetryTimerEvent() {
        MockRetry retry = createMockRetry();
        retry.withInitialRetryInterval(SECONDS, 2);
        retry.withRetryExpirationInterval(SECONDS, 60);
        DateTime dateTime = new DateTime(2000, 1, 1, 12, 0, 0);
        assertEquals(0, retry.getRetryCount());

        retry.currentHistoryEvent = makeTimerFired(dateTime.minusSeconds(1), 100);
        retry.events.add(createTimerStarted(dateTime.minusSeconds(10), 100));
        assertEquals(1, retry.getRetryCount());
        assertEquals(2, retry.nextRetryDelaySeconds());

        retry.events.add(createTimerStarted(dateTime.minusSeconds(20), 90));
        assertEquals(2, retry.getRetryCount());
        assertEquals(4, retry.nextRetryDelaySeconds());

        retry.events.add(createTimerStarted(dateTime.minusSeconds(30), 80));
        assertEquals(3, retry.getRetryCount());
        assertEquals(8, retry.nextRetryDelaySeconds());

        retry.events.add(createTimerStarted(dateTime.minusSeconds(40), 80));
        assertEquals(4, retry.getRetryCount());
        assertEquals(16, retry.nextRetryDelaySeconds());

        //
        retry.withMaximumRetryInterval(SECONDS, 10);
        assertEquals(10, retry.nextRetryDelaySeconds());
    }

    @Test(expected = IllegalStateException.class)
    public void testValidateActionNotSet() {
        new MockRetry().validate();
    }

    @Test
    public void testValidateActionSet() {
        createMockRetry().validate();
    }

    protected MockRetry createMockRetry() {
        MockRetry retry = new MockRetry();
        retry.setAction(new TimerAction("dummy"));
        return retry;
    }

    static class MockRetry extends RetryPolicy {
        List<ActionEvent> events = new ArrayList<>();
        ActionEvent currentHistoryEvent;

        @Override
        List<ActionEvent> getRetryTimerStartedEvents() {
            return events;
        }

        @Override
        protected ActionEvent getCurrentHistoryEvent() {
            return currentHistoryEvent;
        }
    }

    private static ActionEvent makeTimerFired(DateTime time, long startedEventId) {
        HistoryEvent he = new HistoryEvent();
        he.setEventTimestamp(time.toDate());
        he.setEventType(EventType.TimerFired);
        he.setTimerFiredEventAttributes(new TimerFiredEventAttributes()
            .withStartedEventId(startedEventId));
        return new ActionEvent(he);
    }

    private static ActionEvent createTimerStarted(DateTime time, long eventId) {
        HistoryEvent he = new HistoryEvent();
        he.setEventId(eventId);
        he.setEventTimestamp(time.toDate());
        he.setEventType(EventType.TimerStarted);
        he.setTimerStartedEventAttributes(new TimerStartedEventAttributes());
        return new ActionEvent(he);
    }
}