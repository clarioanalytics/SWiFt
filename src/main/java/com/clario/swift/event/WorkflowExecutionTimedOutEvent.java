package com.clario.swift.event;

import com.amazonaws.services.simpleworkflow.model.*;

/**
 * @author George Coller
 */
public class WorkflowExecutionTimedOutEvent extends Event {

    protected WorkflowExecutionTimedOutEvent(HistoryEvent historyEvent) {
        super(historyEvent);
    }

    @Override public EventState getState() { return EventState.INFO; }

    @Override public EventCategory getCategory() { return EventCategory.EXTERNAL; }

    @Override public Long getInitialEventId() { return getEventId(); }

    @Override public boolean isInitialAction() { return false; }

    @Override public String getActionId() { return null; }

    @Override public String getData1() { return null; }

    @Override public String getData2() { return null; }

    public WorkflowExecutionTimedOutEventAttributes getAttributes() {return historyEvent.getWorkflowExecutionTimedOutEventAttributes();}

    public String getTimeoutType() { return getAttributes().getTimeoutType(); }

    public String getChildPolicy() { return getAttributes().getChildPolicy(); }

}
