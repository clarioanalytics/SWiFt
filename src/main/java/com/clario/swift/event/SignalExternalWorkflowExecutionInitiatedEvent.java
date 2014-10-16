package com.clario.swift.event;

import com.amazonaws.services.simpleworkflow.model.*;

/**
 * @author George Coller
 */
public class SignalExternalWorkflowExecutionInitiatedEvent extends Event {

    protected SignalExternalWorkflowExecutionInitiatedEvent(HistoryEvent historyEvent) {
        super(historyEvent);
    }

    @Override public EventState getState() { return EventState.ACTIVE; }

    @Override public EventCategory getCategory() { return EventCategory.ACTION; }

    @Override public Long getInitialEventId() { return getEventId(); }

    @Override public boolean isInitialAction() { return true; }

    @Override public String getActionId() { return getSignalName(); }

    @Override public String getInput() {  return getInput(); } 
    @Override public String getControl() {  return getControl(); } 

    public SignalExternalWorkflowExecutionInitiatedEventAttributes getAttributes() {return historyEvent.getSignalExternalWorkflowExecutionInitiatedEventAttributes();}

    public String getWorkflowId() { return getAttributes().getWorkflowId(); }

    public String getRunId() { return getAttributes().getRunId(); }

    public String getSignalName() { return getAttributes().getSignalName(); }

    public Long getDecisionTaskCompletedEventId() { return getAttributes().getDecisionTaskCompletedEventId(); }

}
