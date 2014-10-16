package com.clario.swift.event;

import com.amazonaws.services.simpleworkflow.model.*;

/**
 * @author George Coller
 */
public class ContinueAsNewWorkflowExecutionFailedEvent extends Event {

    protected ContinueAsNewWorkflowExecutionFailedEvent(HistoryEvent historyEvent) {
        super(historyEvent);
    }

    @Override public EventState getState() { return EventState.CRITICAL; }

    @Override public EventCategory getCategory() { return EventCategory.WORKFLOW; }

    @Override public Long getInitialEventId() { return getEventId(); }

    @Override public boolean isInitialAction() { return false; }

    @Override public String getActionId() { return null; }

    @Override public String getReason() {  return "ContinueAsNewWorkflowExecutionFailed"; }

    @Override public String getDetails() {  return getCause(); }

    public ContinueAsNewWorkflowExecutionFailedEventAttributes getAttributes() {return historyEvent.getContinueAsNewWorkflowExecutionFailedEventAttributes();}

    public  String getCause() { return getAttributes().getCause(); }

    public  Long getDecisionTaskCompletedEventId() { return getAttributes().getDecisionTaskCompletedEventId(); }

}
