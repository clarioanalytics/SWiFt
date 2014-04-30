package com.clario.swift;

import com.amazonaws.services.simpleworkflow.model.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.clario.swift.SwiftUtil.*;
import static com.clario.swift.Workflow.createFailWorkflowExecutionDecision;
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
     * Register workflows added to this poller on Amazon SWF, {@link TypeAlreadyExistsException} are ignored.
     * <p/>
     * Use this poller's domain and taskList.
     */
    public void registerSwfWorkflows() {
        for (Workflow workflow : workflows.values()) {
            try {
                workflow.withDomain(domain);
                workflow.withTaskList(taskList);
                swf.registerWorkflowType(workflow.createRegisterWorkflowTypeRequest());
                log.info(format("Register workflow succeeded %s", workflow));
            } catch (TypeAlreadyExistsException e) {
                log.info(format("Workflow already registered %s", workflow));
            } catch (Throwable e) {
                log.error(format("Register workflow failed %s", workflow));
                throw e;
            }
        }
    }

    /**
     * Add one or more workflows to the poller.
     */
    public void addWorkflows(Workflow... workflows) {
        for (Workflow workflow : workflows) {
            log.info(format("add workflow %s", workflow));
            workflow.withDomain(domain).withTaskList(taskList);
            this.workflows.put(workflow.getKey(), workflow);
        }
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
                    workflow = lookupWorkflow(decisionTask)
                        .withDomain(domain)
                        .withTaskList(taskList)
                        .withWorkflowId(decisionTask.getWorkflowExecution().getWorkflowId())
                        .withRunId(decisionTask.getWorkflowExecution().getRunId());
                    workflow.init();
                }
                workflow.addHistoryEvents(decisionTask.getEvents());
                if (workflow.getCurrentCheckpoint() != null) {
                    request.setNextPageToken(decisionTask.getNextPageToken());
                }
            }
        }
        String workflowId = decisionTask.getWorkflowExecution().getWorkflowId();
        String runId = decisionTask.getWorkflowExecution().getRunId();

        List<Decision> decisions = new ArrayList<>();
        List<HistoryEvent> workflowErrors = workflow.getErrorEvents();

        if (workflowErrors.isEmpty()) {
            try {
                if (log.isInfoEnabled()) {
                    log.info(format("decide workflowId=%s,runId=%s", workflowId, runId));
                }
                workflow.decide(decisions);
                if (log.isInfoEnabled()) {
                    for (Decision decision : decisions) {
                        log.info(format("decide %s : %s", workflowId, decision));
                    }
                }
            } catch (Throwable t) {
                log.error(format("decide workflowId=%s,runId=%s", workflowId, runId), t);
                decisions.add(createFailWorkflowExecutionDecision("decide() threw an unhandled exception", printStackTrace(t)));
            }
        } else {
            String errorMessage = format("decide workflowId=%s,runId=%s schedule activity errors:\n%s", workflowId, runId, join(workflowErrors, "\n"));
            log.error(errorMessage);
            decisions.add(createFailWorkflowExecutionDecision("One or more activities failed during scheduling", errorMessage));
        }

        try {
            swf.respondDecisionTaskCompleted(createRespondDecisionTaskCompletedRequest(decisionTask.getTaskToken(), decisions));
        } catch (Exception e) {
            log.error(format("decide %s %s", workflowId, workflow), e);
        }
    }

    private Workflow lookupWorkflow(DecisionTask decisionTask) {
        String name = decisionTask.getWorkflowType().getName();
        String version = decisionTask.getWorkflowType().getVersion();
        String key = makeKey(name, version);
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
