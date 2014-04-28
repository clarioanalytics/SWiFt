package com.clario.swift.examples;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.clario.swift.SwiftUtil;
import com.clario.swift.Workflow;
import com.clario.swift.action.StartChildWorkflowAction;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * @author George Coller
 */
public class StartChildWorkflow extends Workflow {
    public static final Logger log = LoggerFactory.getLogger(StartChildWorkflow.class);

    public static void main(String[] args) {
        Workflow workflow = new StartChildWorkflow()
            .withDomain("dev-clario")
            .withTaskList("default")
            .withExecutionStartToCloseTimeout(TimeUnit.MINUTES, 30)
            .withTaskStartToCloseTimeout(TimeUnit.MINUTES, 30);
        Config.submit(workflow, "100");
    }

    public StartChildWorkflow() {
        super("Start Child Workflow", "1.0");
    }

    @Override
    public void decide(List<Decision> decisions) {
        String input = getWorkflowInput();

        // Since instances of StartChildWorkflow could be used across multiple calls we need to create a
        // specifically-named childWorkflowId for this workflow run's start child workflow call.
        // Markers come in handy for this since they are persisted in the workflow state.
        // A signal could also be used but then we'd have to wait until the next poll cycle to start the child workflow

        String childWorkflowId = workflowHistory.getMarkers().get("childWorkflowId");
        if (childWorkflowId == null) {
            childWorkflowId = "Child Workflow " + SwiftUtil.timestamp();
            decisions.add(createRecordMarkerDecision("childWorkflowId", childWorkflowId));
        }

        StartChildWorkflowAction startChildWorkflow = createChildWorkflow(childWorkflowId);
        startChildWorkflow.setWorkflow(this);

        if (startChildWorkflow
            .withInput(input)
            .withTaskList(getTaskList())
            .decide(decisions).isSuccess()) {

            String childRunId = startChildWorkflow.getChildRunId();
            Assert.assertNotNull(childRunId);
            log.info("Child run id " + childRunId);
            String data = startChildWorkflow.getOutput();
            decisions.add(createCompleteWorkflowExecutionDecision(data));
        }
    }

    private StartChildWorkflowAction createChildWorkflow(String childWorkflowId) {
        return new StartChildWorkflowAction(childWorkflowId)
            .withNameVersion("Simple Workflow", "2.0")
            .withExecutionStartToCloseTimeout(MINUTES, 10)
            .withTaskStartToCloseTimeout(null, -1);
    }

}
