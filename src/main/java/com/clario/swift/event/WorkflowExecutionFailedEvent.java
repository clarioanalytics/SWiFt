package com.clario.swift.event;

import com.amazonaws.services.simpleworkflow.model.*;

/**
 * @author George Coller
 */
public class WorkflowExecutionFailedEvent extends Event {

    protected WorkflowExecutionFailedEvent(HistoryEvent historyEvent) {
        super(historyEvent);
    }

    @Override public EventState getState() { return EventState.CRITICAL; }

    @Override public EventCategory getCategory() { return EventCategory.WORKFLOW; }

    @Override public Long getInitialEventId() { return getEventId(); }

    @Override public boolean isInitialAction() { return false; }

    @Override public String getActionId() { return null; }

    public WorkflowExecutionFailedEventAttributes getAttributes() {return historyEvent.getWorkflowExecutionFailedEventAttributes();}

    public @Override  String getReason() { return getAttributes().getReason(); }

    public @Override  String getDetails() { return getAttributes().getDetails(); }

    public  Long getDecisionTaskCompletedEventId() { return getAttributes().getDecisionTaskCompletedEventId(); }

}
