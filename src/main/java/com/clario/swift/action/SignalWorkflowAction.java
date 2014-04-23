package com.clario.swift.action;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.amazonaws.services.simpleworkflow.model.DecisionType;
import com.amazonaws.services.simpleworkflow.model.SignalExternalWorkflowExecutionDecisionAttributes;

/**
 * Send a SWF signal to a running workflow (or the current workflow).
 * <p/>
 * Signals, unlike SWF markers, trigger a decision task in SWF on the target workflow.
 *
 * @author George Coller
 */
public class SignalWorkflowAction extends Action {
    private String workflowId;
    private String runId;
    private String input;
    private String control;

    public SignalWorkflowAction(String actionId) {
        super(actionId);
    }

    /**
     * Required identifier of the workflow to be signaled.
     *
     * @see SignalExternalWorkflowExecutionDecisionAttributes#workflowId
     */
    public SignalWorkflowAction withWorkflowId(String workflowId) {
        this.workflowId = workflowId;
        return this;
    }

    /**
     * Optional runId of the workflow to be signaled.
     *
     * @see SignalExternalWorkflowExecutionDecisionAttributes#runId
     */
    public SignalWorkflowAction withRunId(String runId) {
        this.runId = runId;
        return this;
    }

    /**
     * Optional input data attached to the signal.
     *
     * @see SignalExternalWorkflowExecutionDecisionAttributes#input
     */
    public SignalWorkflowAction withInput(String input) {
        this.input = input;
        return this;
    }

    /**
     * Optional data attached to the signal.
     *
     * @see SignalExternalWorkflowExecutionDecisionAttributes#control
     */
    public SignalWorkflowAction withControl(String control) {
        this.control = control;
        return this;
    }

    /**
     * @return a decision of type {@link DecisionType#SignalExternalWorkflowExecution}.
     */
    @Override
    public Decision createInitiateActivityDecision() {
        return new Decision()
            .withDecisionType(DecisionType.SignalExternalWorkflowExecution)
            .withSignalExternalWorkflowExecutionDecisionAttributes(new SignalExternalWorkflowExecutionDecisionAttributes()
                    .withSignalName(getActionId())
                    .withWorkflowId(workflowId)
                    .withRunId(runId)
                    .withControl(control)
                    .withInput(input)
            );
    }
}
