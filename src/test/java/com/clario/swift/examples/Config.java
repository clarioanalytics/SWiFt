package com.clario.swift.examples;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient;
import com.amazonaws.services.simpleworkflow.model.Run;
import com.amazonaws.services.simpleworkflow.model.StartWorkflowExecutionRequest;
import com.clario.swift.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

import static com.clario.swift.SwiftUtil.timestamp;
import static java.lang.String.format;

/**
 * @author George Coller
 */
public class Config {
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

    public AmazonSimpleWorkflow getAmazonSimpleWorkflow() {
        return amazonSimpleWorkflow;
    }

    public int getPoolSize() {
        return poolSize;
    }

    public static void submit(Workflow workflow, String input) {
        String workflowId = format("%s %s", workflow.getName(), timestamp());
        log.info(format("submit workflow: %s", workflowId));

        StartWorkflowExecutionRequest request = workflow.createWorkflowExecutionRequest(workflowId, input);

        log.info(format("Start workflow execution: %s", workflowId));
        Run run = config.getAmazonSimpleWorkflow().startWorkflowExecution(request);
        log.info(format("Started workflow %s", run));
    }
}
