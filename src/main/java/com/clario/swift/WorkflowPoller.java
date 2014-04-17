package com.clario.swift;

import com.amazonaws.services.simpleworkflow.model.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

/**
 * Poll for {@link DecisionTask} on a single domain and task list and ask a registered {@link Workflow} for next decisions.
 * Note: Single threaded, run multiple instances as {@link Runnable} for higher throughput
 *
 * @author George Coller
 */
public class WorkflowPoller extends BasePoller {
    private final Map<String, Workflow> workflows = new LinkedHashMap<>();
    private final String executionContext;

    /**
     * Construct a workflow poller
     *
     * @param id unique identifier of poller for logging purposes
     * @param executionContext optional context to be sent on each {@link RespondDecisionTaskCompletedRequest}
     */
    public WorkflowPoller(String id, String executionContext) {
        super(id);
        this.executionContext = executionContext;
    }

    /**
     * Add a workflow the poller.
     *
     * @param workflow workflow
     */
    public void addWorkflow(Workflow workflow) {
        getLog().info("Register workflow " + workflow.getKey());
        workflows.put(workflow.getKey(), workflow);
    }

    @Override
    protected void poll() {
        // Events are request in newest-first reverse order;
        PollForDecisionTaskRequest request = createPollForDecisionTaskRequest();
        DecisionTask decisionTask = null;
        Workflow workflow = null;

        while (decisionTask == null || decisionTask.getNextPageToken() != null) {
            decisionTask = getSwf().pollForDecisionTask(request);
            if (decisionTask.getTaskToken() == null) {
                getLog().info("poll timeout");
                if (workflow == null) {
                    return;
                }
            } else {
                if (workflow == null) {
                    workflow = initWorkflow(decisionTask);
                }
                workflow.addHistoryEvents(decisionTask.getEvents());
                if (workflow.isMoreHistoryRequired()) {
                    request.setNextPageToken(decisionTask.getNextPageToken());
                }
            }
        }

        getLog().info("decide " + decisionTask.getWorkflowExecution().getWorkflowId() + " " + workflow.getKey());
        List<Decision> decisions = workflow.decide(decisionTask.getWorkflowExecution().getWorkflowId());
        workflow.reset();

        if (decisions.isEmpty()) {
            getLog().info("poll no decisions");
        } else {
            for (Decision decision : decisions) {
                getLog().info("poll decision: " + String.valueOf(decision));
            }
        }
        getSwf().respondDecisionTaskCompleted(createRespondDecisionTaskCompletedRequest(decisionTask.getTaskToken(), decisions));
    }

    private Workflow initWorkflow(DecisionTask decisionTask) {
        String name = decisionTask.getWorkflowType().getName();
        String version = decisionTask.getWorkflowType().getVersion();
        Workflow workflow = workflows.get(makeKey(name, version));
        workflow.reset();
        if (workflow == null) {
            throw new IllegalStateException(format("Received decision task for unregistered workflow %s %s", name, version));
        }
        return workflow;
    }

    public RespondDecisionTaskCompletedRequest createRespondDecisionTaskCompletedRequest(String taskToken, List<Decision> decisions) {
        return new RespondDecisionTaskCompletedRequest()
            .withDecisions(decisions)
            .withTaskToken(taskToken)
            .withExecutionContext(executionContext);
    }

    public PollForDecisionTaskRequest createPollForDecisionTaskRequest() {
        return new PollForDecisionTaskRequest()
            .withDomain(getDomain())
            .withTaskList(new TaskList().withName(getTaskList()))
            .withIdentity(getId())
            .withReverseOrder(true);
    }
}
