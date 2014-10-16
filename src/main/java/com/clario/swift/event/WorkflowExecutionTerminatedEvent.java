package com.clario.swift.event;

import com.amazonaws.services.simpleworkflow.model.*;

/**
 * @author George Coller
 */
public class WorkflowExecutionTerminatedEvent extends Event {

    protected WorkflowExecutionTerminatedEvent(HistoryEvent historyEvent) {
        super(historyEvent);
    }

    @Override public EventState getState() { return EventState.INFO; }

    @Override public EventCategory getCategory() { return EventCategory.EXTERNAL; }

    @Override public Long getInitialEventId() { return getEventId(); }

    @Override public boolean isInitialAction() { return false; }

    @Override public String getActionId() { return null; }

    public WorkflowExecutionTerminatedEventAttributes getAttributes() {return historyEvent.getWorkflowExecutionTerminatedEventAttributes();}

    public @Override  String getReason() { return getAttributes().getReason(); }

    public @Override  String getDetails() { return getAttributes().getDetails(); }

    public  String getChildPolicy() { return getAttributes().getChildPolicy(); }

    public  String getCause() { return getAttributes().getCause(); }

}
