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

    @Override public String getData1() { return getInput(); }

    @Override public String getData2() { return getControl(); }

    public SignalExternalWorkflowExecutionInitiatedEventAttributes getAttributes() {return historyEvent.getSignalExternalWorkflowExecutionInitiatedEventAttributes();}

    public String getWorkflowId() { return getAttributes().getWorkflowId(); }

    public String getRunId() { return getAttributes().getRunId(); }

    public String getSignalName() { return getAttributes().getSignalName(); }

    public String getInput() { return getAttributes().getInput(); }

    public Long getDecisionTaskCompletedEventId() { return getAttributes().getDecisionTaskCompletedEventId(); }

    public String getControl() { return getAttributes().getControl(); }

}
