package com.clario.swift.retry;

import com.amazonaws.services.simpleworkflow.flow.interceptors.ExponentialRetryPolicy;
import com.amazonaws.services.simpleworkflow.model.Decision;
import com.amazonaws.services.simpleworkflow.model.DecisionType;
import com.amazonaws.services.simpleworkflow.model.StartTimerDecisionAttributes;
import com.clario.swift.EventList;
import com.clario.swift.SwiftUtil;
import com.clario.swift.action.Action;

import static com.clario.swift.SwiftUtil.calcTimeoutOrNone;
import static com.clario.swift.action.TimerAction.createStartTimerDecision;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Retry policy that can be registered on an {@link Action} to describe when and how many times
 * the action will be repeated.
 * <p/>
 * A dumbed-down version of {@link ExponentialRetryPolicy} that hopefully is easier to use.
 * <p/>
 * Retries are handled by issuing an SWF {@link DecisionType#StartTimer} {@link Decision} with
 * {@link StartTimerDecisionAttributes#getTimerId()} equal to the action's {@link Action#getActionId()}
 * which means that {@link EventList#selectActionId(String)} returns both action and timer events.
 *
 * @author George Coller
 */
public abstract class RetryPolicy {
    protected String control;
    private String matchesRegEx;

    public RetryPolicy() {
        this.control = format("%s_%d", getClass().getSimpleName(), System.currentTimeMillis());
    }

    /**
     * @param control manually set control value for unit testing.
     */
    protected void setControl(String control) {
        this.control = control;
    }

    /**
     * Called when registered on an {@link Action} to validate the policy parameter settings.
     */
    public abstract void validate();

    /**
     * Calculate the interval to wait in seconds before the next retry should be submitted.
     *
     * @param events {@link Action#getEvents()} for the current action
     *
     * @return interval in seconds, or less than zero to indicate no retry
     */
    abstract int nextRetryDelaySeconds(EventList events);

    public RetryPolicy withStopIfResultMatches(String regEx) {
        this.matchesRegEx = regEx;
        return this;
    }

    public boolean testResultMatches(String... values) {
        for (String value : values) {
            if (matchesRegEx != null && SwiftUtil.defaultIfNull(value, "").matches(matchesRegEx)) {
                return true;
            }
        }
        return false;
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
}
