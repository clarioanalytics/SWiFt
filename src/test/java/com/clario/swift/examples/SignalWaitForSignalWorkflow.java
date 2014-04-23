package com.clario.swift.examples;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.clario.swift.SwiftUtil;
import com.clario.swift.Workflow;
import com.clario.swift.action.SwfAction;
import com.clario.swift.action.SwfSignalWorkflow;
import com.clario.swift.action.SwfStartChildWorkflow;
import com.clario.swift.action.SwfTimer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.clario.swift.SwiftUtil.createCompleteWorkflowExecutionDecision;
import static com.clario.swift.SwiftUtil.createFailWorkflowExecutionDecision;
import static com.clario.swift.action.SwfAction.ActionState.*;
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

    private SwfSignalWorkflow signalSelf = new SwfSignalWorkflow("signalSelf");
    private SwfSignalWorkflow signalChildWorkflow = new SwfSignalWorkflow("signalChildWorkflow");
    private SwfTimer timer = new SwfTimer("timer").withStartToFireTimeout(TimeUnit.SECONDS, 15);

    public SignalWaitForSignalWorkflow() {
        super("Signal Wait For Signal Workflow", "1.0");
    }

    @Override
    public List<SwfAction> getActions() {
        return Arrays.asList(signalSelf, signalChildWorkflow, timer);
    }

    @Override
    public void decide(List<Decision> decisions) {
        String input = getWorkflowInput();

        SwfStartChildWorkflow childWorkflow = createChildWorkflow(decisions);

        // Can't use typical "decide" approach because we want to start it and not block.
        if (childWorkflow.getState() == initial) {
            decisions.add(childWorkflow.withInput(input).withTaskList(getTaskList()).createDecision());
        }
        // Give the child workflow some time to start
        if (timer.decide(decisions)) {
            log.info("Timer finished");
            if (signalChildWorkflow.withWorkflowId(childWorkflow.getId())
                .withInput("999").decide(decisions)) {
                log.info("External workflow signaled");

                // next decision task received should be child workflow complete or error
                if (childWorkflow.getState() == started) {
                    return;
                } else if (childWorkflow.getState() == finish_ok) {
                    String data = childWorkflow.getOutput();
                    decisions.add(createCompleteWorkflowExecutionDecision(data));
                } else {
                    decisions.add(createFailWorkflowExecutionDecision(String.format("%s '%s' error", getClass().getSimpleName(), getWorkflowId()), null));
                }

            }
        }
    }

    private SwfStartChildWorkflow createChildWorkflow(List<Decision> decisions) {
        String childWorkflowId = swfHistory.getMarkers().get("childWorkflowId");
        if (childWorkflowId == null) {
            childWorkflowId = "Child Workflow " + SwiftUtil.timestamp();
            decisions.add(SwiftUtil.createRecordMarkerDecision("childWorkflowId", childWorkflowId));
        }

        SwfStartChildWorkflow startChildWorkflow = createChildWorkflow(childWorkflowId);
        startChildWorkflow.setWorkflow(this);
        return startChildWorkflow;
    }


    private SwfStartChildWorkflow createChildWorkflow(String childWorkflowId) {
        return new SwfStartChildWorkflow(childWorkflowId)
            .withName("Wait For Signal Workflow")
            .withVersion("1.0")
            .withExecutionStartToCloseTimeout(MINUTES, 10)
            .withTaskStartToCloseTimeoutNone();
    }
}
