package com.clario.swift.examples.workflows;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.clario.swift.Workflow;
import com.clario.swift.action.RecordMarkerAction;
import com.clario.swift.action.SignalWorkflowAction;
import com.clario.swift.action.StartChildWorkflowAction;
import com.clario.swift.action.TimerAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.clario.swift.SwiftUtil.createUniqueWorkflowId;
import static com.clario.swift.action.ActionState.initial;
import static com.clario.swift.examples.Config.config;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author George Coller
 */
public class SignalWaitForSignalWorkflow extends Workflow {
    public static final Logger log = LoggerFactory.getLogger(SignalWaitForSignalWorkflow.class);


    public static void main(String[] args) {
        Workflow workflow = new SignalWaitForSignalWorkflow()
            .withDomain(config.getDomain())
            .withTaskList(config.getTaskList())
            .withExecutionStartToCloseTimeout(MINUTES, 30)
            .withTaskStartToCloseTimeout(MINUTES, 30);
        config.submit(workflow, "666");
    }

    private final SignalWorkflowAction signal = new SignalWorkflowAction("signal")
        .withInput("999");

    private final RecordMarkerAction marker = new RecordMarkerAction("childId");

    private final TimerAction timer = new TimerAction("timer")
        .withStartToFireTimeout(SECONDS, 2);

    public SignalWaitForSignalWorkflow() {
        super("Signal Wait For Signal Workflow", "1.0");
        addActions(signal, timer, marker);
    }

    @Override
    public void decide(List<Decision> decisions) {
        log.info("decide");

        if (!marker.isSuccess()) {
            marker.withDetails(createUniqueWorkflowId("Child Workflow")).decide(decisions);
        }
        String childWorkflowId = marker.getOutput();

        StartChildWorkflowAction childWorkflow = new StartChildWorkflowAction(childWorkflowId)
            .withNameVersion("Wait For Signal Workflow", "1.0")
            .withExecutionStartToCloseTimeout(MINUTES, 10)
            .withTaskStartToCloseTimeout(MINUTES, -1)
            .withInput(getWorkflowInput())
            .withTaskList(getTaskList())
            .withCompleteWorkflowOnSuccess();


        childWorkflow.setWorkflow(this); // do not forget this step!

        // Checking state because we don't want childWorkflow to block here
        if (childWorkflow.getState() == initial) {
            log.info("Start child workflow");
            childWorkflow.decide(decisions);
        }

        // Give the child workflow some time to start
        if (timer.decide(decisions).isSuccess()) {
            if (signal.getState() == initial) {
                log.info("Timer finished, send signal"); // log only once
            }
            if (signal.withWorkflowId(childWorkflow.getActionId()).decide(decisions).isSuccess()) {
                // wait until child workflow finishes
                childWorkflow.decide(decisions);
            }
        }
    }
}