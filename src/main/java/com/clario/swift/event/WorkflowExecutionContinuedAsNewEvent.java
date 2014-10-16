package com.clario.swift.event;

import com.amazonaws.services.simpleworkflow.model.*;

/**
 * @author George Coller
 */
public class WorkflowExecutionContinuedAsNewEvent extends Event {

    protected WorkflowExecutionContinuedAsNewEvent(HistoryEvent historyEvent) {
        super(historyEvent);
    }

    @Override public EventState getState() { return EventState.INFO; }

    @Override public EventCategory getCategory() { return EventCategory.EXTERNAL; }

    @Override public Long getInitialEventId() { return getEventId(); }

    @Override public boolean isInitialAction() { return false; }

    @Override public String getActionId() { return null; }


    public WorkflowExecutionContinuedAsNewEventAttributes getAttributes() {return historyEvent.getWorkflowExecutionContinuedAsNewEventAttributes();}

    public Long getDecisionTaskCompletedEventId() { return getAttributes().getDecisionTaskCompletedEventId(); }

    public String getNewExecutionRunId() { return getAttributes().getNewExecutionRunId(); }

    public String getExecutionStartToCloseTimeout() { return getAttributes().getExecutionStartToCloseTimeout(); }

    public String getTaskList() { return getAttributes().getTaskList().getName(); }

    public String getTaskStartToCloseTimeout() { return getAttributes().getTaskStartToCloseTimeout(); }

    public String getChildPolicy() { return getAttributes().getChildPolicy(); }

    public String getWorkflowName() { return getAttributes().getWorkflowType().getName(); }

    public String getWorkflowVersion() { return getAttributes().getWorkflowType().getVersion(); }

}
