package com.clario.swift.event;

import com.amazonaws.services.simpleworkflow.model.*;

/**
 * @author George Coller
 */
public class WorkflowExecutionCancelRequestedEvent extends Event {

    protected WorkflowExecutionCancelRequestedEvent(HistoryEvent historyEvent) {
        super(historyEvent);
    }

    @Override public EventState getState() { return EventState.CRITICAL; }

    @Override public EventCategory getCategory() { return EventCategory.WORKFLOW; }

    @Override public Long getInitialEventId() { return getEventId(); }

    @Override public boolean isInitialAction() { return false; }

    @Override public String getActionId() { return null; }

    @Override public String getReason() {  return "WorkflowExecutionCancelRequested"; }

    @Override public String getDetails() {  return getCause(); }

    public WorkflowExecutionCancelRequestedEventAttributes getAttributes() {return historyEvent.getWorkflowExecutionCancelRequestedEventAttributes();}

    public  String getWorkflowId() { return getAttributes().getExternalWorkflowExecution().getWorkflowId(); }

    public  String getRunId() { return getAttributes().getExternalWorkflowExecution().getRunId(); }

    public  Long getExternalInitiatedEventId() { return getAttributes().getExternalInitiatedEventId(); }

    public  String getCause() { return getAttributes().getCause(); }

}
