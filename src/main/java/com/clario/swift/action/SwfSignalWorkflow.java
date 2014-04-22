package com.clario.swift.action;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.amazonaws.services.simpleworkflow.model.DecisionType;
import com.amazonaws.services.simpleworkflow.model.SignalExternalWorkflowExecutionDecisionAttributes;

/**
 * Send a SWF signal to a running workflow (or the current workflow).
 *
 * @author George Coller
 */
public class SwfSignalWorkflow extends SwfAction {
    private String workflowId;
    private String runId;
    private String input;
    private String control;

    public SwfSignalWorkflow(String id) {
        super(id);
    }

    public SwfSignalWorkflow withWorkflowId(String workflowId) {
        this.workflowId = workflowId;
        return this;
    }

    public SwfSignalWorkflow withRunId(String runId) {
        this.runId = runId;
        return this;
    }

    public SwfSignalWorkflow withInput(String input) {
        this.input = input;
        return this;
    }

    public SwfSignalWorkflow withControl(String control) {
        this.control = control;
        return this;
    }

    @Override
    protected Decision createDecision() {
        return new Decision()
            .withDecisionType(DecisionType.SignalExternalWorkflowExecution)
            .withSignalExternalWorkflowExecutionDecisionAttributes(new SignalExternalWorkflowExecutionDecisionAttributes()
                    .withSignalName(id)
                    .withWorkflowId(workflowId)
                    .withRunId(runId)
                    .withControl(control)
                    .withInput(input)
            );
    }
}
