package com.clario.swift.examples.workflows;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.clario.swift.Workflow;
import com.clario.swift.action.RecordMarkerAction;
import com.clario.swift.action.SignalWorkflowAction;
import com.clario.swift.action.StartChildWorkflowAction;
import com.clario.swift.action.TimerAction;
import com.clario.swift.examples.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.clario.swift.SwiftUtil.createUniqueWorkflowId;
import static com.clario.swift.examples.Config.config;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * This example works with the {@link WaitForSignalWorkflow} example to demonstrate how a
 * workflow can initiate a child workflow, send data to it via a signal, wait for the child to complete,
 * and then continue on.
 *
 * @author George Coller
 * @see StartChildWorkflowAction
 */
public class SignalWaitForSignalWorkflow extends Workflow {
    public static final Logger log = LoggerFactory.getLogger(SignalWaitForSignalWorkflow.class);


    /** Start the workflow by submitting it to SWF. */
    public static void main(String[] args) {
        Workflow workflow = new SignalWaitForSignalWorkflow()
            .withDomain(config().getDomain())
            .withTaskList(config().getTaskList())
            .withExecutionStartToCloseTimeout(MINUTES, 30)
            .withTaskStartToCloseTimeout(MINUTES, 30);
        config().submit(workflow, "666");
    }

    private final StartChildWorkflowAction childWorkflow = new StartChildWorkflowAction("childWorkflow")
                                                               .withNameVersion("Wait For Signal Workflow", "1.0")
                                                               .withExecutionStartToCloseTimeout(MINUTES, 10)
                                                               .withTaskStartToCloseTimeout(null, -1);

    private final SignalWorkflowAction signal = new SignalWorkflowAction("signal").withInput("999");

    private final TimerAction timer = new TimerAction("timer").withStartToFireTimeout(SECONDS, 5);

    public SignalWaitForSignalWorkflow() {
        super("Signal Wait For Signal Workflow", "1.0");
        addActions(signal, timer, childWorkflow);
    }

    @Override
    public void decide(List<Decision> decisions) {
        log.info("decide");

        // Only start the child workflow once
        if (childWorkflow.isNotStarted()) {
            log.info("Start child workflow, give it 5 secs to start up");
            childWorkflow.withInput(getWorkflowInput()).decide(decisions);
        }

        // Give the child workflow some time to start using a timer action
        if (timer.decide(decisions).isSuccess()) {
            if (signal.isNotStarted()) {
                log.info("Timer finished, send signal"); // log only once
            }

            // important to get the child workflow's id, not this workflow's id
            if (signal.withWorkflowId(childWorkflow.getChildWorkflowId()).decide(decisions).isSuccess()) {
                // wait until child workflow finishes
                childWorkflow.withCompleteWorkflowOnSuccess().decide(decisions);

                // Alternatively parent workflow could have continued on with more work here....
            }
        }
    }
}
