package com.clario.swift.examples.workflows;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.amazonaws.services.simpleworkflow.model.WorkflowExecutionAlreadyStartedException;
import com.clario.swift.ActionEvent;
import com.clario.swift.Workflow;
import com.clario.swift.action.ContinueAsNewAction;
import com.clario.swift.examples.Config;

import java.util.List;

import static com.clario.swift.examples.Config.*;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * Example of a never-ending workflow, which is useful for on-going "chron" jobs.
 * <p/>
 * Note: In this example I'm using signals to do the "Continue as New" action but
 * in a real workflow you'd probably use a timer-action.
 * <p/>
 * You can use this class' main method to start the workflow and then the Amazon SWF Console to send signals to it.
 * With each signal the current workflow will be decided "Continue As New", which you'll notice in the polling log as
 * it increments the workflow input with each continuation.  You'll also notice that the runId changes for the workflow
 * after each signal, again demonstrating that the workflow did continue as new.
 *
 * @author George Coller
 */
public class ContinuousWorkflow extends Workflow {
    public static final String WF_ID = "Continuous Workflow Example";
    private static final String WF_NAME = "Continuous Workflow";
    private static final String WF_VERSION = "1.0";

    public static void main(String[] args) throws InterruptedException {
        try {
            Workflow workflow = new ContinuousWorkflow()
                .withDomain(SWIFT_DOMAIN)
                .withTaskList(SWIFT_TASK_LIST)
                .withExecutionStartToCloseTimeout(DAYS, 365)
                .withTaskStartToCloseTimeout(MINUTES, 1);
            Config.getConfig().submit(workflow, WF_ID, "1");
        } catch (WorkflowExecutionAlreadyStartedException e) {
            log.warn(WF_NAME + " is already running");
        }
    }

    private final ContinueAsNewAction continueAsNewAction = new ContinueAsNewAction("continueAsNew");

    public ContinuousWorkflow() {
        super(WF_NAME, WF_VERSION);
        addActions(continueAsNewAction);
    }

    @Override
    public void decide(List<Decision> decisions) {
        List<ActionEvent> signals = getWorkflowHistory().getSignals();
        Integer input = Integer.valueOf(getWorkflowInput());
        if (signals.isEmpty()) {
            log.info("New workflow instance started with input " + input);
        } else {
            input++;
            log.info(String.format("Signal '%s' received, continuing workflow with new input %d", signals.get(0).getActionId(), input));
            continueAsNewAction.withInput(input.toString());
            continueAsNewAction.decide(decisions);
        }
    }
}
