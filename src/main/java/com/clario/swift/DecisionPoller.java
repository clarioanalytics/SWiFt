package com.clario.swift;

import com.amazonaws.services.simpleworkflow.model.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.clario.swift.SwiftUtil.createFailWorkflowExecutionDecision;
import static com.clario.swift.SwiftUtil.join;
import static java.lang.String.format;

/**
 * Poll for {@link DecisionTask} on a single domain and task list and ask a registered {@link Workflow} for next decisions.
 * Note: Single threaded, run multiple instances as {@link Runnable} for higher throughput
 *
 * @author George Coller
 */
public class DecisionPoller extends BasePoller {
    private final Map<String, Workflow> workflows = new LinkedHashMap<>();
    private final String executionContext;

    /**
     * Construct a workflow poller
     *
     * @param id unique identifier of poller for logging purposes
     * @param executionContext optional context to be sent on each {@link RespondDecisionTaskCompletedRequest}
     */
    public DecisionPoller(String id, String domain, String taskList, String executionContext) {
        super(id, domain, taskList);
        this.executionContext = executionContext;
    }

    /**
     * Add a workflow the poller.
     *
     * @param workflow workflow
     */
    public void addWorkflow(Workflow workflow) {
        log.info(format("Register workflow %s", workflow.getWorkflowKey()));
        workflows.put(workflow.getWorkflowKey(), workflow);
    }

    @Override
    protected void poll() {
        // Events are request in newest-first reverse order;
        PollForDecisionTaskRequest request = createPollForDecisionTaskRequest();
        DecisionTask decisionTask = null;
        Workflow workflow = null;

        while (decisionTask == null || decisionTask.getNextPageToken() != null) {
            decisionTask = swf.pollForDecisionTask(request);
            if (decisionTask.getTaskToken() == null) {
                log.info("poll timeout");
                if (workflow == null) {
                    return;
                }
            } else {
                if (workflow == null) {
                    workflow = lookupWorkflow(decisionTask);
                    workflow.reset();
                }
                workflow.addHistoryEvents(decisionTask.getEvents());
                if (workflow.isMoreHistoryRequired()) {
                    request.setNextPageToken(decisionTask.getNextPageToken());
                }
            }
        }

        String workflowId = decisionTask.getWorkflowExecution().getWorkflowId();
        List<Decision> decisions = new ArrayList<>();
        List<String> errors = workflow.getSchedulingErrors();
        if (!errors.isEmpty()) {
            String errorMessage = format("Workflow %s %s schedule activity errors: %s ", workflowId, workflow.getWorkflowKey(), join(errors, ", "));
            log.error(errorMessage);
            decisions.add(createFailWorkflowExecutionDecision("One or more activities failed during scheduling", errorMessage));
        } else {
            if (log.isInfoEnabled()) {
                log.info(format("decide %s %s", workflowId, workflow.getWorkflowKey()));
            }
            workflow.decide(workflowId, decisions);

            if (decisions.isEmpty()) {
                log.info("poll no decisions");
            } else {
                for (Decision decision : decisions) {
                    // TODO: Convert to debug
                    log.info(format("poll decision: %s", decision));
                }
            }
        }

        try {
            swf.respondDecisionTaskCompleted(createRespondDecisionTaskCompletedRequest(decisionTask.getTaskToken(), decisions));
        } catch (Exception e) {
            log.error(format("decide %s %s", workflowId, workflow.getWorkflowKey()), e);
        }
    }


    private Workflow lookupWorkflow(DecisionTask decisionTask) {
        String name = decisionTask.getWorkflowType().getName();
        String version = decisionTask.getWorkflowType().getVersion();
        String key = SwiftUtil.makeKey(name, version);
        Workflow workflow = workflows.get(key);
        if (workflow == null) {
            throw new IllegalStateException(format("Received decision task for unregistered workflow %s", key));
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
            .withDomain(domain)
            .withTaskList(new TaskList().withName(taskList))
            .withIdentity(getId())
            .withReverseOrder(true);
    }
}
