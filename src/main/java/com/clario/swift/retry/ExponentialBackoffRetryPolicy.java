package com.clario.swift.retry;

import com.clario.swift.EventList;
import com.clario.swift.SwiftUtil;
import org.joda.time.DateTime;
import org.joda.time.Days;
import org.joda.time.Hours;
import org.joda.time.Seconds;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.lang.Integer.MAX_VALUE;
import static java.lang.Math.pow;
import static org.joda.time.Seconds.seconds;
import static org.joda.time.Seconds.secondsBetween;

public class ExponentialBackoffRetryPolicy extends RetryPolicy {
    public static final int DEFAULT_INITIAL_RETRY_INTERVAL = 5;
    public static final double DEFAULT_BACKOFF_COEFFICIENT = 2.0;
    protected double backoffCoefficient = DEFAULT_BACKOFF_COEFFICIENT;
    protected int maximumAttempts = MAX_VALUE;
    protected Seconds initialRetryInterval = seconds(DEFAULT_INITIAL_RETRY_INTERVAL);
    protected Seconds maximumRetryInterval = Hours.ONE.toStandardSeconds();
    protected Seconds retryExpirationInterval = Days.days(365).toStandardSeconds();

    /**
     * Interval to wait before the initial retry.
     *
     * @param duration must be greater than zero, defaults to five seconds
     */
    public ExponentialBackoffRetryPolicy withInitialRetryInterval(TimeUnit unit, long duration) {
        initialRetryInterval = calcSeconds(unit, duration);
        return this;
    }

    /**
     * Stop retrying after activity has been running for this duration.
     *
     * @param duration must be greater than zero, defaults to 365 days.
     */
    public ExponentialBackoffRetryPolicy withRetryExpirationInterval(TimeUnit unit, long duration) {
        this.retryExpirationInterval = calcSeconds(unit, duration);
        return this;
    }

    /**
     * Limit the maximum delay time between retries.
     *
     * @param duration must be greater than zero, defaults to one hour.
     */
    public ExponentialBackoffRetryPolicy withMaximumRetryInterval(TimeUnit unit, long duration) {
        this.maximumRetryInterval = calcSeconds(unit, duration);
        return this;
    }

    /**
     * Set the maximum number of retry attempts before stopping.
     *
     * @param maximumAttempts number of attempts, if less than zero will set to default of no maximum attempt count.
     */
    public ExponentialBackoffRetryPolicy withMaximumAttempts(int maximumAttempts) {
        this.maximumAttempts = maximumAttempts > 0 ? maximumAttempts : MAX_VALUE;
        return this;
    }


    public void validate() {
        if (initialRetryInterval.isGreaterThan(maximumRetryInterval)) {
            throw new IllegalStateException("initialRetryInterval > maximumRetryInterval");
        }
        if (initialRetryInterval.isGreaterThan(retryExpirationInterval)) {
            throw new IllegalStateException("initialRetryInterval > retryExpirationInterval");
        }
    }

    public int nextRetryDelaySeconds(EventList events) {
        EventList retryEvents = events.selectRetryCount(getControl());
        if (retryEvents.size() > maximumAttempts) {
            return -1;
        } else if (retryEvents.isEmpty()) {
            return initialRetryInterval.getSeconds();
        } else {
            DateTime firstRetryDate = retryEvents.getLast().getEventTimestamp();
            DateTime currentErrorDate = events.getFirst().getEventTimestamp();

            Seconds secondsElapsed = secondsBetween(firstRetryDate, currentErrorDate);
            Seconds interval = initialRetryInterval.multipliedBy((int) pow(backoffCoefficient, retryEvents.size()));
            if (interval.isGreaterThan(maximumRetryInterval)) {
                interval = maximumRetryInterval;
            }
            return secondsElapsed.plus(interval).isGreaterThan(retryExpirationInterval) ? -1 : interval.getSeconds();
        }
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
        return ExponentialBackoffRetryPolicy.class.getSimpleName() + "{" + SwiftUtil.join(parts, ", ") + "}";
    }
}
