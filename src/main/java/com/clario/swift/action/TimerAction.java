package com.clario.swift.action;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.amazonaws.services.simpleworkflow.model.DecisionType;
import com.amazonaws.services.simpleworkflow.model.StartTimerDecisionAttributes;

import java.util.concurrent.TimeUnit;

import static com.clario.swift.SwiftUtil.calcTimeoutString;

/**
 * Represents an SWF Timer.
 *
 * @author George Coller
 */
public class TimerAction extends Action {
    private String startToFireTimeout;
    private String control;

    public TimerAction(String actionId) {
        super(actionId);
    }

    /**
     * The duration to wait before firing the timer. This field is required.
     * Pass null unit or duration &lt;= 0 for a timeout of NONE.
     *
     * @see StartTimerDecisionAttributes#startToFireTimeout
     */
    public TimerAction withStartToFireTimeout(TimeUnit unit, long duration) {
        startToFireTimeout = calcTimeoutString(unit, duration);
        return this;
    }

    /**
     * Optional control value to add to this timer.
     *
     * @see StartTimerDecisionAttributes#control
     */
    public TimerAction withControl(String control) {
        this.control = control;
        return this;
    }

    /**
     * @return a decision of type {@link DecisionType#StartTimer}.
     */
    @Override
    public Decision createInitiateActivityDecision() {
        return new Decision()
            .withDecisionType(DecisionType.StartTimer)
            .withStartTimerDecisionAttributes(new StartTimerDecisionAttributes()
                .withTimerId(getActionId())
                .withStartToFireTimeout(startToFireTimeout)
                .withControl(control));
    }
}
