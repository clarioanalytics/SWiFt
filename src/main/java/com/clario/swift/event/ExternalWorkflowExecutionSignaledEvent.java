package com.clario.swift.event;

import com.amazonaws.services.simpleworkflow.model.*;

/**
 * @author George Coller
 */
public class ExternalWorkflowExecutionSignaledEvent extends Event {

    protected ExternalWorkflowExecutionSignaledEvent(HistoryEvent historyEvent) {
        super(historyEvent);
    }

    @Override public EventState getState() { return EventState.SUCCESS; }

    @Override public EventCategory getCategory() { return EventCategory.ACTION; }

    @Override public Long getInitialEventId() { return getInitiatedEventId(); }

    @Override public boolean isInitialAction() { return false; }

    @Override public String getActionId() { return null; }

    @Override public String getOutput() {  return getRunId(); }

    public ExternalWorkflowExecutionSignaledEventAttributes getAttributes() {return historyEvent.getExternalWorkflowExecutionSignaledEventAttributes();}

    public  String getWorkflowId() { return getAttributes().getWorkflowExecution().getWorkflowId(); }

    public  String getRunId() { return getAttributes().getWorkflowExecution().getRunId(); }

    public  Long getInitiatedEventId() { return getAttributes().getInitiatedEventId(); }

}
