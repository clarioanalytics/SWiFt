package com.clario.swift.event;

import com.amazonaws.services.simpleworkflow.model.*;

/**
 * @author George Coller
 */
public class SignalExternalWorkflowExecutionFailedEvent extends Event {

    protected SignalExternalWorkflowExecutionFailedEvent(HistoryEvent historyEvent) {
        super(historyEvent);
    }

    @Override public EventState getState() { return EventState.ERROR; }

    @Override public EventCategory getCategory() { return EventCategory.ACTION; }

    @Override public Long getInitialEventId() { return getInitiatedEventId(); }

    @Override public boolean isInitialAction() { return false; }

    @Override public String getActionId() { return null; }

    @Override public String getReason() {  return "SignalExternalWorkflowExecutionFailed"; }

    @Override public String getDetails() {  return getCause(); }

    public SignalExternalWorkflowExecutionFailedEventAttributes getAttributes() {return historyEvent.getSignalExternalWorkflowExecutionFailedEventAttributes();}

    public  String getWorkflowId() { return getAttributes().getWorkflowId(); }

    public  String getRunId() { return getAttributes().getRunId(); }

    public  String getCause() { return getAttributes().getCause(); }

    public  Long getInitiatedEventId() { return getAttributes().getInitiatedEventId(); }

    public  Long getDecisionTaskCompletedEventId() { return getAttributes().getDecisionTaskCompletedEventId(); }

    public @Override  String getControl() { return getAttributes().getControl(); }

}
