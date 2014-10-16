package com.clario.swift.event;

import com.amazonaws.services.simpleworkflow.model.*;

/**
 * @author George Coller
 */
public class RequestCancelExternalWorkflowExecutionInitiatedEvent extends Event {

    protected RequestCancelExternalWorkflowExecutionInitiatedEvent(HistoryEvent historyEvent) {
        super(historyEvent);
    }

    @Override public EventState getState() { return EventState.INFO; }

    @Override public EventCategory getCategory() { return EventCategory.EXTERNAL; }

    @Override public Long getInitialEventId() { return getEventId(); }

    @Override public boolean isInitialAction() { return false; }

    @Override public String getActionId() { return null; }

    @Override public String getControl() {  return getControl(); } 

    public RequestCancelExternalWorkflowExecutionInitiatedEventAttributes getAttributes() {return historyEvent.getRequestCancelExternalWorkflowExecutionInitiatedEventAttributes();}

    public String getWorkflowId() { return getAttributes().getWorkflowId(); }

    public String getRunId() { return getAttributes().getRunId(); }

    public Long getDecisionTaskCompletedEventId() { return getAttributes().getDecisionTaskCompletedEventId(); }

}
