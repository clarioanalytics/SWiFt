package com.clario.swift.action;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.amazonaws.services.simpleworkflow.model.DecisionType;
import com.amazonaws.services.simpleworkflow.model.RequestCancelExternalWorkflowExecutionDecisionAttributes;
import com.clario.swift.TaskType;

/**
 * Issue a cancel request to an external workflow.
 *
 * @author George Coller
 */
public class CancelExternalWorkflowAction extends Action<CancelExternalWorkflowAction> {
    private String workflowId;
    private String runId;
    private String control;

    public CancelExternalWorkflowAction(String actionId) {
        super(actionId);
    }

    public CancelExternalWorkflowAction setWorkflowId(String workflowId) {
        this.workflowId = workflowId;
        return this;
    }

    public CancelExternalWorkflowAction setRunId(String runId) {
        this.runId = runId;
        return this;
    }

    public CancelExternalWorkflowAction setControl(String control) {
        this.control = control;
        return this;
    }

    @Override public TaskType getTaskType() { return TaskType.CANCEL_EXTERNAL_WORKFLOW; }

    @Override public String getControl() {
        return super.getControl();
    }

    @Override protected CancelExternalWorkflowAction thisObject() {
        return this;
    }

    @Override public Decision createInitiateActivityDecision() {
        return new Decision()
            .withDecisionType(DecisionType.RequestCancelExternalWorkflowExecution)
            .withRequestCancelExternalWorkflowExecutionDecisionAttributes(
                new RequestCancelExternalWorkflowExecutionDecisionAttributes()
                    .withWorkflowId(workflowId)
                    .withRunId(runId)
                    .withControl(control)
            );
    }
}
