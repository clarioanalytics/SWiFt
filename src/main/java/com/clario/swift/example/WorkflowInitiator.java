package com.clario.swift.example;

import com.amazonaws.services.simpleworkflow.model.Run;
import com.amazonaws.services.simpleworkflow.model.StartWorkflowExecutionRequest;
import com.amazonaws.services.simpleworkflow.model.TaskList;
import com.amazonaws.services.simpleworkflow.model.WorkflowType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;

import static java.lang.String.format;

/**
 * @author George Coller
 */
public class WorkflowInitiator {
    public static final Logger log = LoggerFactory.getLogger(WorkflowInitiator.class);

    public static void main(String[] args) throws IOException {
        Config config = Config.getConfig();

        StartWorkflowExecutionRequest request = new StartWorkflowExecutionRequest()
            .withDomain("dev-clario")
            .withTaskList(new TaskList()
                .withName("default"))
            .withWorkflowType(new WorkflowType()
                .withName("Demo Workflow")
                .withVersion("1.0"))
            .withWorkflowId(String.format("Demo Workflow run: %s", new Date()))
            .withInput("100")
            .withTagList(new ArrayList<>(Arrays.asList("Demo")));
        log.info("start workflow execution: " + request);
        Run run = config.getAmazonSimpleWorkflow().startWorkflowExecution(request);
        log.info(format("Started workflow %s %s", run, request));
    }

}
