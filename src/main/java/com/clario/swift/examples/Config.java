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

    public static final Config config = new Config();
    private final AmazonSimpleWorkflow amazonSimpleWorkflow;
    private String domain;
    private String taskList;
    private int activityPoolSize = 2;
    private int decisionPoolSize = 2;
    private boolean registerActivities = false;
    private boolean registerWorkflows = false;

    private Config() {
        try {
            Properties p = new Properties();
            p.load(getClass().getClassLoader().getResourceAsStream("config.properties"));
            String id = p.getProperty("amazon.aws.id");
            String key = p.getProperty("amazon.aws.key");
            amazonSimpleWorkflow = new AmazonSimpleWorkflowClient(new BasicAWSCredentials(id, key),
                new ClientConfiguration().withConnectionTimeout(10 * 1000)
            );

            domain = p.getProperty("swf.domain");
            taskList = p.getProperty("swf.task.list");

            activityPoolSize = parseInt(p.getProperty("activity.pool.size"));
            decisionPoolSize = parseInt(p.getProperty("decision.pool.size"));

            registerActivities = parseBoolean(p.getProperty("register.activities"));
            registerWorkflows = parseBoolean(p.getProperty("register.workflows"));
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
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
