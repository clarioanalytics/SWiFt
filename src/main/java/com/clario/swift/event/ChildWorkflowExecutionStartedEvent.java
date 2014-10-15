package com.clario.swift.event;

import com.amazonaws.services.simpleworkflow.model.*;

/**
 * @author George Coller
 */
public class ChildWorkflowExecutionStartedEvent extends Event {

    protected ChildWorkflowExecutionStartedEvent(HistoryEvent historyEvent) {
        super(historyEvent);
    }

    @Override public EventState getState() { return EventState.ACTIVE; }

    @Override public EventCategory getCategory() { return EventCategory.ACTION; }

    @Override public Long getInitialEventId() { return getInitiatedEventId(); }

    @Override public boolean isInitialAction() { return false; }

    @Override public String getActionId() { return null; }

    @Override public String getData1() { return getRunId(); }

    @Override public String getData2() { return null; }

    public ChildWorkflowExecutionStartedEventAttributes getAttributes() {return historyEvent.getChildWorkflowExecutionStartedEventAttributes();}

    public String getWorkflowId() { return getAttributes().getWorkflowExecution().getWorkflowId(); }

    public String getRunId() { return getAttributes().getWorkflowExecution().getRunId(); }

    public String getWorkflowName() { return getAttributes().getWorkflowType().getName(); }

    public String getWorkflowVersion() { return getAttributes().getWorkflowType().getVersion(); }

    public Long getInitiatedEventId() { return getAttributes().getInitiatedEventId(); }

}
