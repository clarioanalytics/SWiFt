package com.clario.swift.examples.workflows;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.amazonaws.services.simpleworkflow.model.WorkflowExecutionAlreadyStartedException;
import com.clario.swift.ActionEvent;
import com.clario.swift.Workflow;
import com.clario.swift.action.ContinueAsNewAction;
import com.clario.swift.action.TimerAction;

import java.util.List;

import static com.clario.swift.examples.Config.config;
import static com.clario.swift.examples.Config.log;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * Example of a never-ending workflow, which is useful for on-going "chron" jobs.
 * <p/>
 * Note: In this example I'm using signals to do the "Continue as New" action but
 * in a real workflow you'd probably use a {@link TimerAction} to continue every X hours or days depending on
 * the chattiness of your workflow.
 * <p/>
 * You can use this class' main method to start the workflow and then the Amazon SWF Console to send signals to it.
 * With each signal the current workflow will be decided "Continue As New" and the new input logged.
 * <p/>
 * Since it is really a new workflow instance you'll have to refresh your SWF console to see it as the prior one
 * will now be terminated.
 *
 * @author George Coller
 */
public class ContinuousWorkflow extends Workflow {
    public static final String WF_ID = "Continuous Workflow Example";
    private static final String WF_NAME = "Continuous Workflow";
    private static final String WF_VERSION = "1.0";

    /** Start the workflow by submitting it to SWF. */
    public static void main(String[] args) throws InterruptedException {
        try {
            Workflow workflow = new ContinuousWorkflow()
                .withDomain(config.getDomain())
                .withTaskList(config.getTaskList())
                .withExecutionStartToCloseTimeout(DAYS, 365)
                .withTaskStartToCloseTimeout(MINUTES, 1);
            config.submit(workflow, WF_ID, "1");
        } catch (WorkflowExecutionAlreadyStartedException ignore) {
            log.warn(WF_NAME, " is already running");
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

        if (signals.isEmpty()) {
            log.info("New workflow instance started with input {}", getWorkflowInput());
        } else {
            ActionEvent signalEvent = signals.get(0);
            String input = signalEvent.getData1();
            continueAsNewAction.withInput(input);
            continueAsNewAction.decide(decisions);
            log.info("Signal '{}' received, continuing workflow with input {}", signalEvent.getActionId(), input);
        }
    }
}
