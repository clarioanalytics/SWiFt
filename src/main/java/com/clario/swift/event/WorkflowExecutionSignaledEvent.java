package com.clario.swift.event;

import com.amazonaws.services.simpleworkflow.model.*;

/**
 * @author George Coller
 */
public class WorkflowExecutionSignaledEvent extends Event {

    protected WorkflowExecutionSignaledEvent(HistoryEvent historyEvent) {
        super(historyEvent);
    }

    @Override public EventState getState() { return EventState.SUCCESS; }

    @Override public EventCategory getCategory() { return EventCategory.SIGNAL; }

    @Override public Long getInitialEventId() { return getEventId(); }

    @Override public boolean isInitialAction() { return false; }

    @Override public String getActionId() { return getSignalName(); }

    @Override public String getData1() { return getInput(); }

    @Override public String getData2() { return null; }

    public WorkflowExecutionSignaledEventAttributes getAttributes() {return historyEvent.getWorkflowExecutionSignaledEventAttributes();}

    public String getSignalName() { return getAttributes().getSignalName(); }

    public String getInput() { return getAttributes().getInput(); }

    public String getWorkflowId() { return getAttributes().getExternalWorkflowExecution().getWorkflowId(); }

    public String getRunId() { return getAttributes().getExternalWorkflowExecution().getRunId(); }

    public Long getExternalInitiatedEventId() { return getAttributes().getExternalInitiatedEventId(); }

}
