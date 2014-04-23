package com.clario.swift.examples;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.clario.swift.SwiftUtil;
import com.clario.swift.Workflow;
import com.clario.swift.action.SignalWorkflowAction;
import com.clario.swift.action.StartChildWorkflowAction;
import com.clario.swift.action.TimerAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.clario.swift.action.Action.State.active;
import static com.clario.swift.action.Action.State.initial;
import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * @author George Coller
 */
public class SignalWaitForSignalWorkflow extends Workflow {
    public static final Logger log = LoggerFactory.getLogger(SignalWaitForSignalWorkflow.class);


    public static void main(String[] args) {
        Workflow workflow = new SignalWaitForSignalWorkflow()
            .withDomain("dev-clario")
            .withTaskList("default")
            .withExecutionStartToCloseTimeout(TimeUnit.MINUTES, 30)
            .withTaskStartToCloseTimeout(TimeUnit.MINUTES, 30);
//        Config.register(workflow);
        Config.submit(workflow, "666");
    }

    private final SignalWorkflowAction signal = new SignalWorkflowAction("signal");
    private final TimerAction timer = new TimerAction("timer").withStartToFireTimeout(TimeUnit.SECONDS, 15);

    public SignalWaitForSignalWorkflow() {
        super("Signal Wait For Signal Workflow", "1.0");
        addActions(signal, timer);
    }

    @Override
    public void decide(List<Decision> decisions) {
        String input = getWorkflowInput();

        StartChildWorkflowAction childWorkflow = createChildWorkflow(decisions);

        // Can't use typical "decide" approach because we want to start it and not block.
        if (childWorkflow.getState() == initial) {
            decisions.add(childWorkflow.withInput(input).withTaskList(getTaskList()).createInitiateActivityDecision());
        }
        // Give the child workflow some time to start
        if (timer.decide(decisions).isSuccess()) {
            log.info("Timer finished");
            if (signal
                .withWorkflowId(childWorkflow.getActionId())
                .withInput("999")
                .decide(decisions).isSuccess()) {
                log.info("External workflow signaled");

                if (childWorkflow.getState() == active) {
                    return;
                } else if (childWorkflow.isSuccess()) {
                    String data = childWorkflow.getOutput();
                    decisions.add(createCompleteWorkflowExecutionDecision(data));
                } else {
                    decisions.add(createFailWorkflowExecutionDecision(String.format("%s '%s' error", getClass().getSimpleName(), getWorkflowId()), null));
                }

            }
        }
    }

    private StartChildWorkflowAction createChildWorkflow(List<Decision> decisions) {
        String childWorkflowId = workflowHistory.getMarkers().get("childWorkflowId");
        if (childWorkflowId == null) {
            childWorkflowId = "Child Workflow " + SwiftUtil.timestamp();
            decisions.add(createRecordMarkerDecision("childWorkflowId", childWorkflowId));
        }

        StartChildWorkflowAction startChildWorkflow = new StartChildWorkflowAction(childWorkflowId)
            .withNameVersion("Wait For Signal Workflow", "1.0")
            .withExecutionStartToCloseTimeout(MINUTES, 10)
            .withTaskStartToCloseTimeout(null, -1);
        startChildWorkflow.setWorkflow(this);
        return startChildWorkflow;
    }


}
