package com.clario.swift.event;

import com.amazonaws.services.simpleworkflow.model.*;

/**
 * @author George Coller
 */
public class WorkflowExecutionStartedEvent extends Event {

    protected WorkflowExecutionStartedEvent(HistoryEvent historyEvent) {
        super(historyEvent);
    }

    @Override public EventState getState() { return EventState.SUCCESS; }

    @Override public EventCategory getCategory() { return EventCategory.WORKFLOW; }

    @Override public Long getInitialEventId() { return getEventId(); }

    @Override public boolean isInitialAction() { return false; }

    @Override public String getActionId() { return null; }

    @Override public String getOutput() {  return getInput(); }

    public WorkflowExecutionStartedEventAttributes getAttributes() {return historyEvent.getWorkflowExecutionStartedEventAttributes();}

    public @Override  String getInput() { return getAttributes().getInput(); }

    public  String getExecutionStartToCloseTimeout() { return getAttributes().getExecutionStartToCloseTimeout(); }

    public  String getTaskStartToCloseTimeout() { return getAttributes().getTaskStartToCloseTimeout(); }

    public  String getChildPolicy() { return getAttributes().getChildPolicy(); }

    public  String getTaskList() { return getAttributes().getTaskList().getName(); }

    public  String getWorkflowName() { return getAttributes().getWorkflowType().getName(); }

    public  String getWorkflowVersion() { return getAttributes().getWorkflowType().getVersion(); }

    public  String getContinuedExecutionRunId() { return getAttributes().getContinuedExecutionRunId(); }

    public  String getWorkflowId() { return getAttributes().getParentWorkflowExecution().getWorkflowId(); }

    public  String getRunId() { return getAttributes().getParentWorkflowExecution().getRunId(); }

    public  Long getParentInitiatedEventId() { return getAttributes().getParentInitiatedEventId(); }

}
