package com.clario.swift.action;

import com.amazonaws.services.simpleworkflow.model.*;
import com.clario.swift.Workflow;

import static com.clario.swift.SwiftUtil.*;
import static java.lang.String.format;

/**
 * Initiate a continue workflow as new action.
 * <p/>
 * This action is used for endless workflows, which should be terminated and continued as new
 * periodically to ensure that the history log does not get too large.
 *
 * @author George Coller
 */
public class ContinueAsNewAction extends Action<ContinueAsNewAction> {

    private String input;

    public ContinueAsNewAction(String actionId) {
        super(actionId);
    }

    /**
     * @see StartChildWorkflowExecutionDecisionAttributes#input
     */
    public ContinueAsNewAction withInput(String input) {
        this.input = assertMaxLength(input, MAX_INPUT_LENGTH);
        return this;
    }

    @Override
    public Decision createInitiateActivityDecision() {
        Workflow workflow = getWorkflow();
        if (workflow == null) {
            throw new IllegalStateException("Workflow required before calling method");
        }
        if (input == null) {
            input = workflow.getWorkflowInput();
        }
        return new Decision()
            .withDecisionType(DecisionType.ContinueAsNewWorkflowExecution)
            .withContinueAsNewWorkflowExecutionDecisionAttributes(new ContinueAsNewWorkflowExecutionDecisionAttributes()
                    .withInput(defaultIfNull(input, workflow.getWorkflowInput()))
                    .withTaskList(new TaskList().withName(workflow.getTaskList()))
                    .withExecutionStartToCloseTimeout(defaultIfNull(workflow.getExecutionStartToCloseTimeout(), "NONE"))
                    .withTaskStartToCloseTimeout(defaultIfNull(workflow.getTaskStartToCloseTimeout(), "NONE"))
                    .withChildPolicy(workflow.getChildPolicy())
                    .withTagList(workflow.getTags())
                    .withWorkflowTypeVersion(workflow.getVersion())
            );
    }

    @Override
    protected ContinueAsNewAction thisObject() { return this; }

    @Override
    public String toString() {
        Workflow workflow = getWorkflow();
        String name = workflow == null ? "?" : workflow.getName();
        String version = workflow == null ? "?" : workflow.getName();
        return format("%s %s %s", getClass().getSimpleName(), getActionId(), makeKey(name, version));
    }
}
