package com.clario.swift.action;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.amazonaws.services.simpleworkflow.model.DecisionType;
import com.clario.swift.TaskType;
import com.clario.swift.Workflow;
import com.clario.swift.event.EventState;

import java.util.ArrayList;
import java.util.List;

import static com.amazonaws.services.simpleworkflow.model.DecisionType.ScheduleActivityTask;
import static com.clario.swift.event.EventState.*;
import static java.util.Arrays.asList;

/**
 * @author George Coller
 */
public class MockAction extends Action<MockAction> {
    private String input;
    private Decision decision;
    private boolean nonFinalDecisionMade;

    private List<EventState> eventStates = new ArrayList<>(asList(NOT_STARTED, SUCCESS));
    private String control;

    public MockAction(String actionId) {
        super(actionId);
    }

    public MockAction withInput(String input) {
        this.input = input;
        return this;
    }

    public void setEventStates(EventState... eventStates) {
        this.eventStates = new ArrayList<>(asList(eventStates));
    }

    @Override public String getInput() {
        return input;
    }

    @Override public String getOutput() {
        assert input != null : getActionId() + ": assert input string is not null on getOutput()";
        if (input.isEmpty()) {
            return getActionId();
        } else {
            return input + "->" + getActionId();
        }
    }

    /**
     * Control will be used for unit test validation.
     */
    @Override public String getControl() {
        return control;
    }

    @Override public Action decide(List<Decision> decisions) {
        if (getState() == NOT_STARTED) {
            decision = createInitiateActivityDecision();
            decisions.add(decision);
            control = getOutput();
            nonFinalDecisionMade = true;
        }
        if (getState() == ERROR) {
            decision = Workflow.createFailWorkflowExecutionDecision(getActionId(), "error", "");
            control = decision.getFailWorkflowExecutionDecisionAttributes().getReason().replaceAll("\n", "");
            decisions.add(decision);
        }
        return this;
    }

    @Override public EventState getState() {
        assert !eventStates.isEmpty() : getActionId() + ": assert eventStates not empty";
        return eventStates.get(0);
    }

    @Override public TaskType getTaskType() {
        return TaskType.ACTIVITY;
    }

    @Override protected MockAction thisObject() {
        return this;
    }

    @Override public Decision createInitiateActivityDecision() {
        assert decision == null;
        return new Decision().withDecisionType(ScheduleActivityTask);
    }

    public Decision getDecision() {
        return decision;
    }

    public void nextState() {
        if (nonFinalDecisionMade) {
            nonFinalDecisionMade = false;
            eventStates.remove(0);
        }
    }
}
