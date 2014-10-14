package com.clario.swift.action;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.amazonaws.services.simpleworkflow.model.DecisionType;
import com.amazonaws.services.simpleworkflow.model.StartTimerDecisionAttributes;
import com.clario.swift.Event;

import java.util.concurrent.TimeUnit;

import static com.amazonaws.services.simpleworkflow.model.DecisionType.StartTimer;
import static com.clario.swift.Event.State.INITIAL;
import static com.clario.swift.SwiftUtil.*;
import static java.lang.String.format;

/**
 * Represents an SWF Timer.
 *
 * @author George Coller
 */
public class TimerAction extends Action<TimerAction> {
    private String startToFireTimeout;
    private String control;

    public TimerAction(String actionId) {
        super(actionId);
    }

    /**
     * The duration to wait before firing the timer. This field is required.
     * Pass null unit or duration &lt;= 0 for a timeout of 365 days.
     *
     * @see StartTimerDecisionAttributes#startToFireTimeout
     */
    public TimerAction withStartToFireTimeout(TimeUnit unit, long duration) {
        startToFireTimeout = calcTimeoutOrYear(unit, duration);
        return this;
    }

    public String getStartToFireTimeout() { return startToFireTimeout; }

    /**
     * Optional control value to add to this timer.
     *
     * @see StartTimerDecisionAttributes#control
     */
    public TimerAction withControl(String control) {
        this.control = assertMaxLength(control, MAX_CONTROL_LENGTH);
        return this;
    }

    public String getControl() { return control; }

    /**
     * @return a decision of type {@link DecisionType#StartTimer}.
     */
    @Override
    public Decision createInitiateActivityDecision() {
        return createStartTimerDecision(getActionId(), startToFireTimeout, control);
    }

    /**
     * Create SWF {@link DecisionType#StartTimer} {@link Decision}.
     */
    public static Decision createStartTimerDecision(String timerId, String startToFireTimeout, String control) {
        return new Decision()
            .withDecisionType(StartTimer)
            .withStartTimerDecisionAttributes(new StartTimerDecisionAttributes()
                .withTimerId(timerId)
                .withStartToFireTimeout(startToFireTimeout)
                .withControl(trimToMaxLength(control, MAX_CONTROL_LENGTH)));
    }

    @Override
    protected TimerAction thisObject() { return this; }

    @Override
    public String toString() {
        return format("%s %s", getClass().getSimpleName(), startToFireTimeout);
    }
}
