package com.clario.swift.event;

import com.amazonaws.services.simpleworkflow.model.*;

/**
 * @author George Coller
 */
public class WorkflowExecutionCanceledEvent extends Event {

    protected WorkflowExecutionCanceledEvent(HistoryEvent historyEvent) {
        super(historyEvent);
    }

    @Override public EventState getState() { return EventState.INFO; }

    @Override public EventCategory getCategory() { return EventCategory.EXTERNAL; }

    @Override public Long getInitialEventId() { return getEventId(); }

    @Override public boolean isInitialAction() { return false; }

    @Override public String getActionId() { return null; }


    public WorkflowExecutionCanceledEventAttributes getAttributes() {return historyEvent.getWorkflowExecutionCanceledEventAttributes();}

    public String getDetails() { return getAttributes().getDetails(); }

    public Long getDecisionTaskCompletedEventId() { return getAttributes().getDecisionTaskCompletedEventId(); }

}
