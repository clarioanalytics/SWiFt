package com.clario.swift.event;

import com.amazonaws.services.simpleworkflow.model.*;

/**
 * @author George Coller
 */
public class StartChildWorkflowExecutionInitiatedEvent extends Event {

    protected StartChildWorkflowExecutionInitiatedEvent(HistoryEvent historyEvent) {
        super(historyEvent);
    }

    @Override public EventState getState() { return EventState.ACTIVE; }

    @Override public EventCategory getCategory() { return EventCategory.ACTION; }

    @Override public Long getInitialEventId() { return getEventId(); }

    @Override public boolean isInitialAction() { return true; }

    @Override public String getActionId() { return getWorkflowId(); }

    @Override public String getInput() {  return getInput(); } 
    @Override public String getControl() {  return getControl(); } 

    public StartChildWorkflowExecutionInitiatedEventAttributes getAttributes() {return historyEvent.getStartChildWorkflowExecutionInitiatedEventAttributes();}

    public String getWorkflowId() { return getAttributes().getWorkflowId(); }

    public String getWorkflowName() { return getAttributes().getWorkflowType().getName(); }

    public String getWorkflowVersion() { return getAttributes().getWorkflowType().getVersion(); }

    public String getExecutionStartToCloseTimeout() { return getAttributes().getExecutionStartToCloseTimeout(); }

    public String getTaskList() { return getAttributes().getTaskList().getName(); }

    public Long getDecisionTaskCompletedEventId() { return getAttributes().getDecisionTaskCompletedEventId(); }

    public String getChildPolicy() { return getAttributes().getChildPolicy(); }

    public String getTaskStartToCloseTimeout() { return getAttributes().getTaskStartToCloseTimeout(); }

}
