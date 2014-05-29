package com.clario.swift.examples;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient;
import com.amazonaws.services.simpleworkflow.model.Run;
import com.amazonaws.services.simpleworkflow.model.SignalWorkflowExecutionRequest;
import com.amazonaws.services.simpleworkflow.model.StartWorkflowExecutionRequest;
import com.amazonaws.services.simpleworkflow.model.WorkflowExecution;
import com.clario.swift.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

import static com.clario.swift.SwiftUtil.createUniqueWorkflowId;
import static java.lang.String.format;

/**
 * @author George Coller
 */
public class Config {
    public static final String SWIFT_DOMAIN = "dev-clario-swift";
    public static final String SWIFT_TASK_LIST = "default";
    public static final Logger log = LoggerFactory.getLogger(Config.class);

    private static final Config config = new Config();
    private final AmazonSimpleWorkflow amazonSimpleWorkflow;
    private int poolSize;

    private Config() {
        try {
            Properties p = new Properties();
            p.load(getClass().getClassLoader().getResourceAsStream("config.properties"));
            String id = p.getProperty("amazon.aws.id");
            String key = p.getProperty("amazon.aws.key");
            poolSize = Integer.parseInt(p.getProperty("poolSize"));
            amazonSimpleWorkflow = new AmazonSimpleWorkflowClient(new BasicAWSCredentials(id, key),
                new ClientConfiguration().withConnectionTimeout(10 * 1000)
            );
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public static Config getConfig() {
        return config;
    }

    public static AmazonSimpleWorkflow getSWF() {
        return config.amazonSimpleWorkflow;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public static WorkflowExecution submit(Workflow workflow, String workflowId, String input) {
        log.info(format("submit workflow: %s", workflowId));

        workflow.withTags("Swift");
        StartWorkflowExecutionRequest request = workflow.createWorkflowExecutionRequest(workflowId, input);

        log.info(format("Start workflow execution: %s", workflowId));
        Run run = getSWF().startWorkflowExecution(request);
        log.info(format("Started workflow %s", run));
        return new WorkflowExecution().withWorkflowId(workflowId).withRunId(run.getRunId());

    }

    public static WorkflowExecution submit(Workflow workflow, String input) {
        String workflowId = createUniqueWorkflowId(workflow.getName());
        return submit(workflow, workflowId, input);
    }

    public static void signal(String domain, String workflowId, String runId, String name, String input) {
        log.info(format("Signal workflow %s with %s %s", workflowId, name, input));
        getSWF().signalWorkflowExecution(new SignalWorkflowExecutionRequest()
                .withDomain(domain)
                .withWorkflowId(workflowId)
                .withRunId(runId)
                .withSignalName(name)
                .withInput(input)
        );
    }


}
