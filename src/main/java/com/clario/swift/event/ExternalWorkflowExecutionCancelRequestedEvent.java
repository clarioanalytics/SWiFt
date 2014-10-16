package com.clario.swift.event;

import com.amazonaws.services.simpleworkflow.model.*;

/**
 * @author George Coller
 */
public class ExternalWorkflowExecutionCancelRequestedEvent extends Event {

    protected ExternalWorkflowExecutionCancelRequestedEvent(HistoryEvent historyEvent) {
        super(historyEvent);
    }

    @Override public EventState getState() { return EventState.INFO; }

    @Override public EventCategory getCategory() { return EventCategory.EXTERNAL; }

    @Override public Long getInitialEventId() { return getEventId(); }

    @Override public boolean isInitialAction() { return false; }

    @Override public String getActionId() { return null; }


    public ExternalWorkflowExecutionCancelRequestedEventAttributes getAttributes() {return historyEvent.getExternalWorkflowExecutionCancelRequestedEventAttributes();}

    public String getWorkflowId() { return getAttributes().getWorkflowExecution().getWorkflowId(); }

    public String getRunId() { return getAttributes().getWorkflowExecution().getRunId(); }

    public Long getInitiatedEventId() { return getAttributes().getInitiatedEventId(); }

}
