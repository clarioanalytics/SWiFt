package com.clario.swift.action;

import com.amazonaws.services.simpleworkflow.model.*;
import com.clario.swift.Workflow;

import static com.clario.swift.SwiftUtil.*;
import static java.lang.String.format;

/**
 * Initiate a continue workflow as new action, which stops the current workflow and creates a new one with the same
 * {@link Workflow#workflowId}.
 * <p/>
 * Useful for creating on-going cron-like workflows.  Such workflows need to be continued occasionally so that
 * the event history does not exceed SWF's maximum allowed count.
 *
 * @author George Coller
 */
public class ContinueAsNewAction extends Action<ContinueAsNewAction> {

    private String input;

    public ContinueAsNewAction(String actionId) {
        super(actionId);
    }

    /**
     * Allows for starting the new workflow with a new input value.
     *
     * @see StartChildWorkflowExecutionDecisionAttributes#input
     */
    public ContinueAsNewAction withInput(String input) {
        this.input = assertMaxLength(input, MAX_INPUT_LENGTH);
        return this;
    }

    public String getInput() {
        return isInitial() ? input : super.getInput();
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
                    .withExecutionStartToCloseTimeout(defaultIfNull(workflow.getExecutionStartToCloseTimeout(), SWF_TIMEOUT_YEAR))
                    .withTaskStartToCloseTimeout(defaultIfNull(workflow.getTaskStartToCloseTimeout(), SWF_TIMEOUT_NONE))
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
