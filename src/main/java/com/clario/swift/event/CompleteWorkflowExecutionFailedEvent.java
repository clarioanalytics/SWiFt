package com.clario.swift.event;

import com.amazonaws.services.simpleworkflow.model.*;

/**
 * @author George Coller
 */
public class CompleteWorkflowExecutionFailedEvent extends Event {

    protected CompleteWorkflowExecutionFailedEvent(HistoryEvent historyEvent) {
        super(historyEvent);
    }

    @Override public EventState getState() { return EventState.CRITICAL; }

    @Override public EventCategory getCategory() { return EventCategory.WORKFLOW; }

    @Override public Long getInitialEventId() { return getEventId(); }

    @Override public boolean isInitialAction() { return false; }

    @Override public String getActionId() { return null; }

    @Override public String getReason() {  return "CompleteWorkflowExecutionFailed"; }

    @Override public String getDetails() {  return getCause(); }

    public CompleteWorkflowExecutionFailedEventAttributes getAttributes() {return historyEvent.getCompleteWorkflowExecutionFailedEventAttributes();}

    public  String getCause() { return getAttributes().getCause(); }

    public  Long getDecisionTaskCompletedEventId() { return getAttributes().getDecisionTaskCompletedEventId(); }

}