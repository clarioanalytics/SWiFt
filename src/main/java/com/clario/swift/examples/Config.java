package com.clario.swift.examples;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient;
import com.amazonaws.services.simpleworkflow.model.Run;
import com.amazonaws.services.simpleworkflow.model.StartWorkflowExecutionRequest;
import com.amazonaws.services.simpleworkflow.model.WorkflowExecution;
import com.clario.swift.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.ExecutorService;

import static com.clario.swift.SwiftUtil.createUniqueWorkflowId;
import static java.lang.Boolean.parseBoolean;
import static java.lang.Integer.parseInt;
import static java.lang.String.format;

/**
 * Config class used by example workflows and {@link ActivityPollerPool} and {@link DecisionPollerPool}.
 * <p/>
 * See the project's README.md file for more info.
 *
 * @author George Coller
 */
public class Config {
    public static final Logger log = LoggerFactory.getLogger(Config.class);
    private static Config config;

    private final AmazonSimpleWorkflow amazonSimpleWorkflow;
    private String domain;
    private String taskList;
    private int activityPoolSize = 2;
    private int decisionPoolSize = 2;
    private boolean registerActivities = false;
    private boolean registerWorkflows = false;

    private Config() {
        Properties p = new Properties();
        try {
            p.load(getClass().getClassLoader().getResourceAsStream("config.properties"));
        } catch (Exception ignored) {
            throw new IllegalStateException("Cannot init example workflow configuration, config.properties file");
        }
        String id = p.getProperty("amazon.aws.id").trim();
        String key = p.getProperty("amazon.aws.key").trim();
        amazonSimpleWorkflow = new AmazonSimpleWorkflowClient(new BasicAWSCredentials(id, key),
            new ClientConfiguration().withConnectionTimeout(10 * 1000)
        );

        domain = p.getProperty("swf.domain").trim();
        taskList = p.getProperty("swf.task.list").trim();

        activityPoolSize = parseInt(p.getProperty("activity.pollers.pool.size").trim());
        decisionPoolSize = parseInt(p.getProperty("decision.pollers.pool.size").trim());

        registerActivities = parseBoolean(p.getProperty("activity.pollers.register").trim());
        registerWorkflows = parseBoolean(p.getProperty("decision.pollers.register").trim());
    }

    public static synchronized Config config() {
        if (config == null) {
            config = new Config();
        }
        return config;
    }
    
    public void registerShutdownMethod(ExecutorService service) {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down pool and exiting.");
            try {
                config().getSWF().shutdown();
            } finally {
                service.shutdownNow();
            }
        }));     
    }


    public AmazonSimpleWorkflow getSWF() { return amazonSimpleWorkflow; }

    public String getDomain() { return domain; }

    public String getTaskList() { return taskList; }

    public int getActivityPoolSize() { return activityPoolSize; }

    public int getDecisionPoolSize() { return decisionPoolSize; }

    public boolean isRegisterActivities() { return registerActivities; }

    public boolean isRegisterWorkflows() { return registerWorkflows; }

    public WorkflowExecution submit(Workflow workflow, String workflowId, String input) {
        log.info(format("submit workflow: %s", workflowId));

        workflow.withTags("Swift");
        StartWorkflowExecutionRequest request = workflow.createWorkflowExecutionRequest(workflowId, input);

        log.info(format("Start workflow execution: %s", workflowId));
        Run run = getSWF().startWorkflowExecution(request);
        log.info(format("Started workflow %s", run));
        return new WorkflowExecution().withWorkflowId(workflowId).withRunId(run.getRunId());
    }

    public WorkflowExecution submit(Workflow workflow, String input) {
        String workflowId = createUniqueWorkflowId(workflow.getName());
        return submit(workflow, workflowId, input);
    }
}
