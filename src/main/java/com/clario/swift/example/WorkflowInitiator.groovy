package com.clario.swift.example

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient
import com.amazonaws.services.simpleworkflow.model.Run
import com.amazonaws.services.simpleworkflow.model.StartWorkflowExecutionRequest
import com.amazonaws.services.simpleworkflow.model.TaskList
import com.amazonaws.services.simpleworkflow.model.WorkflowType
import groovy.util.logging.Slf4j

/**
 * @author George Coller
 */
@Slf4j
public class WorkflowInitiator {

    public static void main(String[] args) {
        Properties p = new Properties()
        p.load(ActivityWorker.class.getResourceAsStream("config.properties"))
        String id = p.getProperty("amazon.aws.id")
        String key = p.getProperty("amazon.aws.key")
        AmazonSimpleWorkflow swf = new AmazonSimpleWorkflowClient(new BasicAWSCredentials(id, key))

        def request = new StartWorkflowExecutionRequest()
                .withDomain('dev-clario')
                .withTaskList(new TaskList().withName("default"))
                .withWorkflowType(new WorkflowType().withName("Demo Workflow").withVersion("1.0"))
                .withWorkflowId("Demo Workflow run: ${new Date()}")
                .withInput('100')
                .withTagList(['Demo'])
        Run run = swf.startWorkflowExecution(request)
        log.info("Started workflow $run $request")
    }

}