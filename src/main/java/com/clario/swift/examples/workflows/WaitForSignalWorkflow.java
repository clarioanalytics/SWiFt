package com.clario.swift.examples.workflows;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.clario.swift.Workflow;
import com.clario.swift.action.ActivityAction;
import com.clario.swift.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.amazonaws.services.simpleworkflow.model.EventType.WorkflowExecutionSignaled;
import static com.clario.swift.examples.Config.config;
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

    /** Start the workflow by submitting it to SWF. */
    public static void main(String[] args) {
        Workflow workflow = new WaitForSignalWorkflow()
            .withDomain(config().getDomain())
            .withTaskList(config().getTaskList())
            .withExecutionStartToCloseTimeout(MINUTES, 30)
            .withTaskStartToCloseTimeout(MINUTES, 30);
        config().submit(workflow, "100");
    }

    private final ActivityAction step1 = new ActivityAction("step1", "Activity X", "1.0")
        .withScheduleToCloseTimeout(MINUTES, 60)
        .withStartToCloseTimeout(MINUTES, -1)  // setting -1 is same as "NONE", or no timeout
        .withStartToCloseTimeout(MINUTES, -1);

    public WaitForSignalWorkflow() {
        super("Wait For Signal Workflow", "1.0");
        withExecutionStartToCloseTimeout(MINUTES, 30);
        addActions(step1);
    }

    @Override
    public void decide(List<Decision> decisions) {
        // Wait until a signal is received, then do a activity
        Event signal = getEvents().selectEventType(WorkflowExecutionSignaled).getFirst();

        if (signal == null) {
            log.info("No signal received yet");
        } else {
            String input = signal.getInput();

            if (step1.isNotStarted()) {
                // Only log the first time
                log.info("Signal '{}' received with value {}", signal.getActionId(), input);
            }

            if (step1
                .withInput(input)
                .withCompleteWorkflowOnSuccess()
                .decide(decisions)
                .isSuccess()) {

                // a complete workflow decision should now be in decisions list.
                log.info("Signal received and step1 finished");
            }
        }
    }
}
