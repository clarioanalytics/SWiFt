package com.clario.swift.examples.workflows;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.clario.swift.ActionEvent;
import com.clario.swift.Workflow;
import com.clario.swift.action.ActionState;
import com.clario.swift.action.ActivityAction;
import com.clario.swift.examples.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.clario.swift.examples.Config.SWIFT_DOMAIN;
import static com.clario.swift.examples.Config.SWIFT_TASK_LIST;
import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * Example of a workflow that pauses until an external signal tells it to resume.
 * <p/>
 * Workflow will start and then pause for up to 30 minutes.
 * <p/>
 * Use the Amazon SWF console to signal the workflow and it will resume and finish.
 * <p/>
 * NOTE: signal name for this example doesn't matter, input should be an integer value.
 * <p/>
 * For more fun submit multiple times and then signal them all at once from the SWF console.
 *
 * @author George Coller
 */
public class WaitForSignalWorkflow extends Workflow {
    public static final Logger log = LoggerFactory.getLogger(WaitForSignalWorkflow.class);

    public static void main(String[] args) {
        Workflow workflow = new WaitForSignalWorkflow()
            .withDomain(SWIFT_DOMAIN)
            .withTaskList(SWIFT_TASK_LIST)
            .withExecutionStartToCloseTimeout(MINUTES, 30)
            .withTaskStartToCloseTimeout(MINUTES, 30);
        Config.getConfig().submit(workflow, "100");
    }

    private final ActivityAction step1 = new ActivityAction("step1", "Activity X", "1.0")
        .withScheduleToCloseTimeout(MINUTES, 60)
        .withStartToCloseTimeout(MINUTES, -1)
        .withStartToCloseTimeout(MINUTES, -1);

    public WaitForSignalWorkflow() {
        super("Wait For Signal Workflow", "1.0");
        withExecutionStartToCloseTimeout(MINUTES, 30);
        addActions(step1);
    }

    @Override
    public void decide(List<Decision> decisions) {
        // Wait until a signal is received, then do a activity
        List<ActionEvent> signals = getWorkflowHistory().getSignals();
        if (signals.isEmpty()) {
            log.info("No signal received yet");
        } else {
            ActionEvent signalEvent = signals.get(0);
            String input = signalEvent.getData1();

            // Only log the first time here
            if (step1.getState() == ActionState.initial) {
                log.info("Signal '{}' received with value {}", signalEvent.getActionId(), input);
            }

            if (step1
                .withInput(input)
                .withCompleteWorkflowOnSuccess()
                .decide(decisions)
                .isSuccess()) {
                log.info("Signal received and step1 finished");
            }
        }
    }
}
