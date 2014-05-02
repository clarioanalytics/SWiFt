package com.clario.swift.action;

import com.amazonaws.services.simpleworkflow.flow.interceptors.ExponentialRetryPolicy;
import com.amazonaws.services.simpleworkflow.model.EventType;
import com.clario.swift.ActionHistoryEvent;
import org.joda.time.DateTime;
import org.joda.time.Seconds;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.amazonaws.services.simpleworkflow.model.EventType.TimerFired;
import static java.lang.Integer.MAX_VALUE;
import static java.lang.Math.pow;
import static org.joda.time.Seconds.seconds;
import static org.joda.time.Seconds.secondsBetween;

/**
 * Exponential retry policy that can be used with Swift actions.
 * <p/>
 * A dumbed-down version of {@link ExponentialRetryPolicy} with Swift 'with' methods.
 *
 * @author George Coller
 */
public class RetryPolicy {
    public static final int DEFAULT_INITIAL_RETRY_INTERVAL = 5;
    private Action<?> action;
    protected double backoffCoefficient = 2.0;
    protected int maximumAttempts = MAX_VALUE;
    protected Seconds initialRetryInterval = seconds(DEFAULT_INITIAL_RETRY_INTERVAL);
    protected Seconds maximumRetryInterval = seconds(MAX_VALUE);
    protected Seconds retryExpirationInterval = seconds(MAX_VALUE);

    void setAction(Action<?> action) { this.action = action; }

    /**
     * Interval to wait before the initial retry.
     *
     * @param duration must be greater than zero, defaults to five seconds
     */
    public RetryPolicy withInitialRetryInterval(TimeUnit unit, long duration) {
        initialRetryInterval = calcSeconds(unit, duration);
        return this;
    }


    /**
     * Stop retrying after this limit.
     *
     * @param duration negative value sets no limit.  Default is no limit.
     */
    public RetryPolicy withRetryExpirationInterval(TimeUnit unit, long duration) {
        this.retryExpirationInterval = calcSeconds(unit, duration);
        return this;
    }

    /**
     * Limit the maximum delay time between retries.
     *
     * @param duration negative value sets no limit.  Default is no limit.
     */
    public RetryPolicy withMaximumRetryInterval(TimeUnit unit, long duration) {
        this.maximumRetryInterval = calcSeconds(unit, duration);
        return this;
    }

    /**
     * Set the maximum number of retry attempts before stopping.
     *
     * @param maximumAttempts default is no maximum
     */
    public RetryPolicy withMaximumAttempts(int maximumAttempts) {
        this.maximumAttempts = maximumAttempts > 0 ? maximumAttempts : MAX_VALUE;
        return this;
    }

    /**
     * Called to validate the policy parameter settings and to ensure an action has been set.
     * <p/>
     * Policy:
     * <ul>
     * <li>{@link #initialRetryInterval} &lt;= {@link #maximumRetryInterval} if set</li>
     * <li>{@link #initialRetryInterval} &lt;= {@link #retryExpirationInterval} if set</li>
     * </ul>
     * <p/>
     *
     * @see ExponentialRetryPolicy#validate()
     */
    public void validate() {
        assertActionSet();
        if (initialRetryInterval.isGreaterThan(maximumRetryInterval)) {
            throw new IllegalStateException("initialRetryInterval > maximumRetryInterval");
        }
        if (initialRetryInterval.isGreaterThan(retryExpirationInterval)) {
            throw new IllegalStateException("initialRetryInterval > retryExpirationInterval");
        }
    }

    /**
     * Return any {@link EventType#TimerStarted} events that are retry events.
     *
     * @throws IllegalStateException if no action set.
     */
    List<ActionHistoryEvent> getRetryTimerStartedEvents() {
        assertActionSet();
        return action.getWorkflow().getWorkflowHistory().filterRetryTimerStartedEvents(action.getActionId());
    }

    /**
     * @return number of retries attempted for this policy's action
     * @throws IllegalStateException if no action set.
     */
    public int getRetryCount() {
        assertActionSet();
        return getRetryTimerStartedEvents().size();
    }

    /**
     * Calculate if the current event represents a retry timer event.
     *
     * @return true, if current event is a retry timer event
     * @throws IllegalStateException if no action set.
     */
    boolean isRetryTimerEvent(ActionHistoryEvent currentEvent) {
        assertActionSet();
        if (TimerFired == currentEvent.getType()) {
            Long eventId = currentEvent.getInitialEventId();
            for (ActionHistoryEvent event : getRetryTimerStartedEvents()) {
                if (eventId.equals(event.getEventId())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Calculate the interval to wait in seconds before the next retry should be submitted.
     *
     * @return interval in seconds, or less than zero to indicate no retry
     * @throws IllegalStateException if no action set.
     */
    public int nextRetryDelaySeconds() {
        List<ActionHistoryEvent> retryEvents = getRetryTimerStartedEvents();
        int retryCount = retryEvents.size();
        if (retryCount > maximumAttempts) {
            return -1;
        } else if (retryCount == 0) {
            return initialRetryInterval.getSeconds();
        } else {
            DateTime firstRetryDate = retryEvents.get(retryEvents.size() - 1).getEventTimestamp();
            DateTime currentErrorDate = getCurrentHistoryEvent().getEventTimestamp();

            Seconds secondsElapsed = secondsBetween(firstRetryDate, currentErrorDate);
            Seconds interval = initialRetryInterval.multipliedBy((int) pow(backoffCoefficient, retryCount - 1));
            if (interval.isGreaterThan(maximumRetryInterval)) {
                interval = maximumRetryInterval;
            }
            return secondsElapsed.plus(interval).isGreaterThan(retryExpirationInterval) ? -1 : interval.getSeconds();
        }
    }

    // Exposed for unit testing
    protected ActionHistoryEvent getCurrentHistoryEvent() {
        return action.getCurrentHistoryEvent();
    }

    private void assertActionSet() {
        if (action == null) {
            throw new IllegalStateException("Action must be set before calling method");
        }
    }

    private static Seconds calcSeconds(TimeUnit unit, long duration) {
        return seconds(duration > 0 ? (int) unit.toSeconds(duration) : MAX_VALUE);
    }

    @Override
    public String toString() {
        return "ActionRetryPolicy{" +
            "initialRetryInterval=" + initialRetryInterval +
            ", backoffCoefficient=" + backoffCoefficient +
            ", maximumRetryInterval=" + maximumRetryInterval +
            ", maximumAttempts=" + maximumAttempts +
            ", retryExpirationInterval=" + retryExpirationInterval +
            '}';
    }
}
