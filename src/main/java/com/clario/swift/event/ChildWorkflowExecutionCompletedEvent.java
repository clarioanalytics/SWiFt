package com.clario.swift.event;

import com.amazonaws.services.simpleworkflow.model.*;

/**
 * @author George Coller
 */
public class ChildWorkflowExecutionCompletedEvent extends Event {

    protected ChildWorkflowExecutionCompletedEvent(HistoryEvent historyEvent) {
        super(historyEvent);
    }

    @Override public EventState getState() { return EventState.SUCCESS; }

    @Override public EventCategory getCategory() { return EventCategory.ACTION; }

    @Override public Long getInitialEventId() { return getInitiatedEventId(); }

    @Override public boolean isInitialAction() { return false; }

    @Override public String getActionId() { return null; }

    @Override public String getOutput() {  return getResult(); } 

    public ChildWorkflowExecutionCompletedEventAttributes getAttributes() {return historyEvent.getChildWorkflowExecutionCompletedEventAttributes();}

    public String getWorkflowId() { return getAttributes().getWorkflowExecution().getWorkflowId(); }

    public String getRunId() { return getAttributes().getWorkflowExecution().getRunId(); }

    public String getWorkflowName() { return getAttributes().getWorkflowType().getName(); }

    public String getWorkflowVersion() { return getAttributes().getWorkflowType().getVersion(); }

    public String getResult() { return getAttributes().getResult(); }

    public Long getInitiatedEventId() { return getAttributes().getInitiatedEventId(); }

    public Long getStartedEventId() { return getAttributes().getStartedEventId(); }

}
