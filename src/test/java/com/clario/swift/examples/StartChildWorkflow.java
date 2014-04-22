package com.clario.swift.examples;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.clario.swift.SwiftUtil;
import com.clario.swift.Workflow;
import com.clario.swift.action.SwfAction;
import com.clario.swift.action.SwfStartChildWorkflow;
import org.junit.Assert;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
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
//        Config.register(workflow);
        Config.submit(workflow, "100");
    }

    private final SwfStartChildWorkflow startChildWorkflow = new SwfStartChildWorkflow("Child Workflow " + SwiftUtil.timestamp())
        .withName("Simple Workflow")
        .withVersion("2.0")
        .withExecutionStartToCloseTimeout(MINUTES, 10)
        .withTaskStartToCloseTimeoutNone();

    public StartChildWorkflow() {
        super("Start Child Workflow", "1.0");
    }

    @Override
    public List<SwfAction> getActions() {
        return asList((SwfAction) startChildWorkflow);
    }

    @Override
    public void decide(List<Decision> decisions) {
        String input = getWorkflowInput();
        if (startChildWorkflow
            .withInput(input)
            .withTaskList(getTaskList())
            .decide(decisions)) {

            String childRunId = startChildWorkflow.getChildRunId();
            Assert.assertNotNull(childRunId);
            log.info("Child run id " + childRunId);
            String data = startChildWorkflow.getOutput();
            decisions.add(SwiftUtil.createCompleteWorkflowExecutionDecision(data));
        }
    }
}
