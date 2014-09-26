package com.clario.swift.action;

import com.amazonaws.services.simpleworkflow.flow.interceptors.ExponentialRetryPolicy;
import com.amazonaws.services.simpleworkflow.model.Decision;
import com.amazonaws.services.simpleworkflow.model.EventType;
import com.amazonaws.services.simpleworkflow.model.RespondActivityTaskFailedRequest;
import com.clario.swift.ActionEvent;
import com.clario.swift.SwiftUtil;
import org.joda.time.DateTime;
import org.joda.time.Seconds;
import org.slf4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.concurrent.TimeUnit;

import static com.amazonaws.services.simpleworkflow.model.EventType.TimerFired;
import static com.amazonaws.services.simpleworkflow.model.EventType.TimerStarted;
import static java.lang.Integer.MAX_VALUE;
import static java.lang.Math.pow;
import static java.lang.String.format;
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
    public static final String RETRY_CONTROL_VALUE = "--  SWiFt Retry Control Value --";
    public static final int DEFAULT_INITIAL_RETRY_INTERVAL = 5;
    public static final double DEFAULT_BACKOFF_COEFFICIENT = 2.0;
    private Action<?> action;
    protected String stopIfErrorMatchesRegEx = null;
    protected double backoffCoefficient = DEFAULT_BACKOFF_COEFFICIENT;
    protected int maximumAttempts = MAX_VALUE;
    protected Seconds initialRetryInterval = seconds(DEFAULT_INITIAL_RETRY_INTERVAL);
    protected Seconds maximumRetryInterval = seconds(MAX_VALUE);
    protected Seconds retryExpirationInterval = seconds(MAX_VALUE);
    protected boolean retryOnSuccess = false;

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
     * Set a regular expression that will stop retries if it matches against the reason or details of the action that caused the error.
     * <p/>
     * Note: When a Swift action throws an exception the message is recorded in {@link RespondActivityTaskFailedRequest#getReason}
     * and the stack-trace is recorded in {@link RespondActivityTaskFailedRequest#getDetails} by the activity poller.
     * <p/>
     * Possible usages:
     * <ul>
     * <li>Set match regular expression to ".*IllegalArgumentException.*" to stop retries if a {@link java.lang.IllegalArgumentException} was thrown by an action.</li>
     * <li>Set match regular expression to a special string your Activity will include in an error message</li>
     * </ul>
     *
     * @see String#matches(String)
     */
    public RetryPolicy withStopIfErrorMatches(String regularExpression) {
        this.stopIfErrorMatchesRegEx = regularExpression;
        return this;
    }

    public RetryPolicy withRetryOnSuccess(boolean value) {
        this.retryOnSuccess = value;
        return this;
    }

    public boolean isRetryOnSuccess() { return retryOnSuccess; }

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
     * @see ExponentialRetryPolicy#validate
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
    List<ActionEvent> getRetryTimerStartedEvents() {
        assertActionSet();
        List<ActionEvent> events = action.filterEvents(TimerStarted);
        ListIterator<ActionEvent> iter = events.listIterator();
        while (iter.hasNext()) {
            ActionEvent event = iter.next();
            if (!RETRY_CONTROL_VALUE.equals(event.getData1())) {
                iter.remove();
            }
        }
        return events;
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
     * Calculate if the current event represents a retry event.
     * A retry event indicates that the action should be retried.
     *
     * @return true, if current event is a retry event
     * @throws IllegalStateException if no action set.
     */
    boolean isRetryActionEvent(ActionEvent currentEvent) {
        assertActionSet();
        if (TimerFired == currentEvent.getType()) {
            Long eventId = currentEvent.getInitialEventId();
            for (ActionEvent event : getRetryTimerStartedEvents()) {
                if (eventId.equals(event.getEventId())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Make a decision using the current action state and this policy.
     * A retry decision is made by creating a {@link TimerAction}
     * with the same actionId as the original action.
     *
     * @param decisions list of decisions for current polling
     *
     * @return true, if a retry decision was added, otherwise false
     * @throws IllegalStateException if no action set.
     */
    public boolean decide(List<Decision> decisions) {
        assertActionSet();
        Logger log = action.getLog();
        ActionEvent currentHistoryEvent = getCurrentHistoryEvent();
        String reason = SwiftUtil.defaultIfEmpty(currentHistoryEvent.getData1(), "");
        String details = SwiftUtil.defaultIfEmpty(currentHistoryEvent.getData2(), "");
        if (stopIfErrorMatchesRegEx != null) {
            if (reason.matches(stopIfErrorMatchesRegEx)) {
                log.info(format("%s no more attempts. matched reason: %s", this, reason));
                return false;
            }
            if (details.matches(stopIfErrorMatchesRegEx)) {
                log.info(format("%s no more attempts. matched details: %s", this, details));
                return false;
            }
        }
        String description = reason + (details.isEmpty() ? "" : "\n") + details;

        int delaySeconds = nextRetryDelaySeconds();
        if (delaySeconds >= 0) {
            TimerAction timer = new TimerAction(action.getActionId())
                .withStartToFireTimeout(TimeUnit.SECONDS, delaySeconds)
                .withControl(RETRY_CONTROL_VALUE);
            log.info(format("%s retry after %d seconds: %s", this, delaySeconds, description));
            decisions.add(timer.createInitiateActivityDecision());
            return true;
        } else {
            log.info(format("%s no more attempts: %s", this, description));
            return false;
        }
    }

    /**
     * Calculate the interval to wait in seconds before the next retry should be submitted.
     *
     * @return interval in seconds, or less than zero to indicate no retry
     * @throws IllegalStateException if no action set.
     */
    public int nextRetryDelaySeconds() {
        List<ActionEvent> retryEvents = getRetryTimerStartedEvents();
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
    protected ActionEvent getCurrentHistoryEvent() {
        return action.getCurrentEvent();
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
        if (stopIfErrorMatchesRegEx != null) {
            parts.add("stopIfReasonMatchesRegEx=" + stopIfErrorMatchesRegEx);
        }
        return RetryPolicy.class.getSimpleName() + "{" + SwiftUtil.join(parts, ", ") + "}";
    }
}
