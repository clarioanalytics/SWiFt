package com.clario.swift.event;

import com.amazonaws.services.simpleworkflow.model.*;

/**
 * @author George Coller
 */
public class SignalExternalWorkflowExecutionFailedEvent extends Event {

    protected SignalExternalWorkflowExecutionFailedEvent(HistoryEvent historyEvent) {
        super(historyEvent);
    }

    @Override public EventState getState() { return EventState.CRITICAL; }

    @Override public EventCategory getCategory() { return EventCategory.WORKFLOW; }

    @Override public Long getInitialEventId() { return getEventId(); }

    @Override public boolean isInitialAction() { return false; }

    @Override public String getActionId() { return null; }

    @Override public String getData1() { return getCause(); }

    @Override public String getData2() { return getControl(); }

    public SignalExternalWorkflowExecutionFailedEventAttributes getAttributes() {return historyEvent.getSignalExternalWorkflowExecutionFailedEventAttributes();}

    public String getWorkflowId() { return getAttributes().getWorkflowId(); }

    public String getRunId() { return getAttributes().getRunId(); }

    public String getCause() { return getAttributes().getCause(); }

    public Long getInitiatedEventId() { return getAttributes().getInitiatedEventId(); }

    public Long getDecisionTaskCompletedEventId() { return getAttributes().getDecisionTaskCompletedEventId(); }

    public String getControl() { return getAttributes().getControl(); }

}
