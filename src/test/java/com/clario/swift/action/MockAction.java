package com.clario.swift.action;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.amazonaws.services.simpleworkflow.model.ScheduleActivityTaskDecisionAttributes;
import com.clario.swift.TaskType;
import com.clario.swift.event.EventState;

import java.util.List;

import static java.lang.String.format;

/**
 * @author George Coller
 */
public class MockAction extends Action<MockAction> {
    private EventState eventState;
    private String input;
    private Decision decision;

    public MockAction(String actionId) {
        this(actionId, EventState.INITIAL);
    }

    public MockAction(String actionId, EventState eventState) {
        super(actionId);
        this.eventState = eventState;
        this.decision = new Decision().withDecisionType(format("Mock %s", actionId));
    }

    public MockAction withInput(String input) {
        this.input = input;
        return this;
    }

    @Override public String getInput() {
        return input;
    }

    @Override public Action decide(List<Decision> decisions) {
        if (eventState == EventState.INITIAL) {
            decisions.add(decision);
        }
        return this;
    }

    public void setEventState(EventState eventState) {
        this.eventState = eventState;
    }

    @Override public EventState getState() {
        return eventState;
    }

    @Override public TaskType getTaskType() {
        return TaskType.ACTIVITY;
    }

    @Override protected MockAction thisObject() {
        return this;
    }

    @Override public Decision createInitiateActivityDecision() {
        return new Decision().withScheduleActivityTaskDecisionAttributes(new ScheduleActivityTaskDecisionAttributes());
    }

    public Decision getDecision() {
        return decision;
    }
}
