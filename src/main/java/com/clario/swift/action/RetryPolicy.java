package com.clario.swift.action;

import com.amazonaws.services.simpleworkflow.flow.interceptors.ExponentialRetryPolicy;
import com.amazonaws.services.simpleworkflow.model.Decision;
import com.amazonaws.services.simpleworkflow.model.DecisionType;
import com.amazonaws.services.simpleworkflow.model.EventType;
import com.amazonaws.services.simpleworkflow.model.StartTimerDecisionAttributes;
import com.clario.swift.EventList;
import com.clario.swift.SwiftUtil;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Hours;
import org.joda.time.Seconds;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.clario.swift.SwiftUtil.calcTimeoutOrNone;
import static com.clario.swift.action.TimerAction.createStartTimerDecision;
import static java.lang.Integer.MAX_VALUE;
import static java.lang.Math.pow;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.joda.time.Seconds.seconds;
import static org.joda.time.Seconds.secondsBetween;

/**
 * Retry policy that can be registered on an {@link Action} to describe when and how many times
 * the action will be repeated on success or retried on failure.
 * <p/>
 * A dumbed-down version of {@link ExponentialRetryPolicy} that hopefully is easier to use.
 * <p/>
 * Retries are handled by issuing an SWF {@link DecisionType#StartTimer} {@link Decision} with
 * {@link StartTimerDecisionAttributes#getTimerId()} equal to the action's {@link Action#getActionId()}
 * and {@link StartTimerDecisionAttributes#getControl()} equal to this instance's {@link #getControl()} value.
 * which means that {@link EventList#selectActionId(String)} returns both action and timer events.
 * <p/>
 * Retry polices can be registered to retry both when an {@link Action} succeeds (for cron-like actions) and when
 * it errors.  The only caveat is that the <code>RetryPolicy</code> instance that is registered on {@link Action#withOnSuccessRetryPolicy(RetryPolicy)}
 * have a different control value than the one registered on {@link Action#withOnErrorRetryPolicy(RetryPolicy)}.
 * This is because the action needs to differentiate between each retry policy's timer events.
 * <p/>
 * <pre>
 * Suggested Usage:
 * - Separate RetryPolicy instance for each {@link Action} in a workflow receiving the retry policy
 * - Use the Action's actionId for the RetryPolicy control value
 * - If you use both an error and success retry policy for a given action, each should have its own control value.
 * </pre>
 *
 * @author George Coller
 */
public class RetryPolicy {
    public static final int DEFAULT_INITIAL_RETRY_INTERVAL = 5;
    public static final double DEFAULT_BACKOFF_COEFFICIENT = 2.0;
    protected double backoffCoefficient = DEFAULT_BACKOFF_COEFFICIENT;
    protected int maximumAttempts = MAX_VALUE;
    protected Seconds initialRetryInterval = seconds(DEFAULT_INITIAL_RETRY_INTERVAL);
    protected Seconds maximumRetryInterval = Hours.ONE.toStandardSeconds();
    protected Seconds retryExpirationInterval = Days.days(365).toStandardSeconds();
    protected String control;
    private RetryPolicyTerminator retryPolicyOnErrorTerminator;

    /**
     * Create a new instance with a defined control value.
     * <p/>
     * The control value is used to match {@link EventType#TimerStarted} records that were issued
     * from this instance.
     * <p/>
     * This means that generally you'll want a unique control value for any action receiving a retry policy.
     * <p>
     * <pre>
     * Suggested Usage:
     * - New instance for each unique {@link Action} in a workflow receiving the retry policy
     * - Can use the Action's actionId for the control value
     * - If you create both an error and success retry policy for a single action, each policy should have its own control value.
     * </pre>
     *
     * @param control custom control value
     */
    public RetryPolicy(String control) {
        this.control = control;
    }

    /**
     * Called when registered on an {@link Action} to validate the policy parameter settings.
     */
    public void validate() {
        if (initialRetryInterval.isGreaterThan(maximumRetryInterval)) {
            throw new IllegalStateException("initialRetryInterval > maximumRetryInterval");
        }
        if (initialRetryInterval.isGreaterThan(retryExpirationInterval)) {
            throw new IllegalStateException("initialRetryInterval > retryExpirationInterval");
        }
    }

    /**
     * Calculate the interval to wait in seconds before the next retry should be submitted.
     *
     * @param events {@link Action#getEvents()} for the current action
     *
     * @return interval in seconds, or less than zero to indicate no retry
     */
    public int nextRetryDelaySeconds(EventList events) {
        EventList retryEvents = events.selectRetryCount(getControl());
        if (retryEvents.size() >= maximumAttempts) {
            return -1;
        } else if (retryEvents.isEmpty()) {
            return initialRetryInterval.getSeconds();
        } else {
            DateTime firstRetryDate = retryEvents.getLast().getEventTimestamp();
            DateTime currentErrorDate = events.getFirst().getEventTimestamp();

            Seconds secondsElapsed = secondsBetween(firstRetryDate, currentErrorDate);
            Seconds interval;
            try {
                interval = initialRetryInterval.multipliedBy((int) pow(backoffCoefficient, retryEvents.size()));
                if (interval.isGreaterThan(maximumRetryInterval)) {
                    interval = maximumRetryInterval;
                }
            } catch (ArithmeticException ignore) {
                interval = maximumRetryInterval;
            }
            return secondsElapsed.plus(interval).isGreaterThan(retryExpirationInterval) ? -1 : interval.getSeconds();
        }
    }

    public RetryPolicy withRetryTerminator(RetryPolicyTerminator retryPolicyOnErrorTerminator) {
        this.retryPolicyOnErrorTerminator = retryPolicyOnErrorTerminator;
        return this;
    }

    public boolean testStopRetrying(String output) {
        return retryPolicyOnErrorTerminator != null && retryPolicyOnErrorTerminator.shouldStopRetrying(output);
    }

    /**
     * Calculate if another retry decision should be made.
     *
     * @param actionId {@link Action#getActionId()}
     * @param events {@link Action#getEvents()}
     *
     * @return {@link DecisionType#StartTimer} if another retry should be attempted, null if no more retries
     */
    public Decision calcNextDecision(String actionId, EventList events) {
        int delaySeconds = nextRetryDelaySeconds(events);
        if (delaySeconds < 0) {
            return null;
        } else {
            return createStartTimerDecision(actionId, calcTimeoutOrNone(SECONDS, delaySeconds), control);
        }
    }

    public String getControl() { return control; }


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
     * Stop retrying after activity has been running for this duration.
     *
     * @param duration must be greater than zero, defaults to 365 days.
     */
    public RetryPolicy withRetryExpirationInterval(TimeUnit unit, long duration) {
        this.retryExpirationInterval = calcSeconds(unit, duration);
        return this;
    }

    /**
     * No backoff, sets {@link #withInitialRetryInterval} and {@link #withMaximumRetryInterval} to the same value.
     *
     * @param duration must be greater than zero
     */
    public RetryPolicy withFixedRetryInterval(TimeUnit unit, long duration) {
        withInitialRetryInterval(unit, duration);
        withMaximumRetryInterval(unit, duration);
        return this;
    }

    /**
     * Limit the maximum delay time between retries.
     *
     * @param duration must be greater than zero, defaults to one hour.
     */
    public RetryPolicy withMaximumRetryInterval(TimeUnit unit, long duration) {
        this.maximumRetryInterval = calcSeconds(unit, duration);
        return this;
    }

    /**
     * Set the maximum number of retry attempts before stopping.
     *
     * @param maximumAttempts number of attempts, if less than zero will set to default of no maximum attempt count.
     */
    public RetryPolicy withMaximumAttempts(int maximumAttempts) {
        this.maximumAttempts = maximumAttempts > 0 ? maximumAttempts : MAX_VALUE;
        return this;
    }


    private static Seconds calcSeconds(TimeUnit unit, long duration) {
        if (duration <= 0) {
            throw new IllegalArgumentException("Duration " + duration + " not a positive integer");
        }
        return seconds((int) unit.toSeconds(duration));
    }

    @Override
    public String toString() {
        List<String> parts = new ArrayList<String>();
        if (initialRetryInterval.getSeconds() != DEFAULT_INITIAL_RETRY_INTERVAL) {
            parts.add("initialRetryInterval=" + initialRetryInterval);
        }
        if (backoffCoefficient != DEFAULT_BACKOFF_COEFFICIENT) {
            parts.add("backoffCoefficient=" + backoffCoefficient);
        }
        if (maximumAttempts != MAX_VALUE) {
            parts.add("maximumAttempts=" + maximumAttempts);
        }
        if (retryExpirationInterval.getSeconds() != MAX_VALUE) {
            parts.add("retryExpirationInterval=" + retryExpirationInterval);
        }
        return RetryPolicy.class.getSimpleName() + "{" + SwiftUtil.join(parts, ", ") + "}";
    }
}
