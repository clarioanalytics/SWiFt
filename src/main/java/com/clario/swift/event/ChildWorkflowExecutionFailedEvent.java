package com.clario.swift.event;

import com.amazonaws.services.simpleworkflow.model.*;

/**
 * @author George Coller
 */
public class ChildWorkflowExecutionFailedEvent extends Event {

    protected ChildWorkflowExecutionFailedEvent(HistoryEvent historyEvent) {
        super(historyEvent);
    }

    @Override public EventState getState() { return EventState.ERROR; }

    @Override public EventCategory getCategory() { return EventCategory.ACTION; }

    @Override public Long getInitialEventId() { return getInitiatedEventId(); }

    @Override public boolean isInitialAction() { return false; }

    @Override public String getActionId() { return null; }

    public ChildWorkflowExecutionFailedEventAttributes getAttributes() {return historyEvent.getChildWorkflowExecutionFailedEventAttributes();}

    public  String getWorkflowId() { return getAttributes().getWorkflowExecution().getWorkflowId(); }

    public  String getRunId() { return getAttributes().getWorkflowExecution().getRunId(); }

    public  String getWorkflowName() { return getAttributes().getWorkflowType().getName(); }

    public  String getWorkflowVersion() { return getAttributes().getWorkflowType().getVersion(); }

    public @Override  String getReason() { return getAttributes().getReason(); }

    public @Override  String getDetails() { return getAttributes().getDetails(); }

    public  Long getInitiatedEventId() { return getAttributes().getInitiatedEventId(); }

    public  Long getStartedEventId() { return getAttributes().getStartedEventId(); }

}
