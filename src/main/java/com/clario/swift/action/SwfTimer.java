package com.clario.swift.action;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.amazonaws.services.simpleworkflow.model.DecisionType;
import com.amazonaws.services.simpleworkflow.model.StartTimerDecisionAttributes;

import java.util.concurrent.TimeUnit;

/**
 * @author George Coller
 */
public class SwfTimer extends SwfAction {
    private String startToFireTimeout;
    private String control;

    public SwfTimer(String timerId) {
        super(timerId);
    }

    /**
     * The duration to wait before firing the timer. This field is required.
     *
     * @see StartTimerDecisionAttributes#startToFireTimeout
     */
    public SwfTimer withStartToFireTimeout(TimeUnit unit, long duration) {
        assertTrue(duration >= 0, "duration must be > 0");
        String string = Long.toString(unit.toSeconds(duration));
        assertStringLength(string, 1, 8, "start to fire timeout");
        this.startToFireTimeout = string;
        return this;
    }

    public SwfTimer withControl(String value) {
        this.control = control;
        return this;
    }

    @Override
    protected Decision createDecision() {
        return new Decision()
            .withDecisionType(DecisionType.StartTimer)
            .withStartTimerDecisionAttributes(new StartTimerDecisionAttributes()
                .withTimerId(id)
                .withStartToFireTimeout(startToFireTimeout)
                .withControl(control));
    }
}
