package com.clario.swift;

import com.amazonaws.services.simpleworkflow.model.*;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Poll for {@link DecisionTask} on a single domain and task list.
 * Note: Single threaded, run multiple instances as {@link Runnable} for higher throughput
 *
 * @author George Coller
 */
public class WorkflowPoller extends BasePoller {
    private final Map<String, List<DecisionStep>> workflows = new LinkedHashMap<>();
    private final HistoryInspector historyInspector = new HistoryInspector();

    /**
     * Optional context to be sent on each decision task completed event
     *
     * @see com.amazonaws.services.simpleworkflow.model.RespondDecisionTaskCompletedRequest#executionContext
     */
    private String executionContext;

    public WorkflowPoller(String id) {
        super(id);
    }

    /**
     * Add a workflow the poller.
     *
     * @param name workflow name
     * @param version workflow version
     * @param steps decision steps for the workflow
     */
    public void addWorkflow(String name, String version, List<DecisionStep> steps) {
        String key = BasePoller.makeKey(name, version);
        getLog().info("Register activity " + key);
        for (DecisionStep step : steps) {
            step.setHistoryInspector(historyInspector);
        }

        workflows.put(key, steps);
    }

    @Override
    protected void poll() {
        PollForDecisionTaskRequest request = createPollForDecisionTaskRequest();
        DecisionTask decisionTask = null;
        historyInspector.clear();

        while (historyInspector.getDecisionGroup() < 1 && (decisionTask == null || decisionTask.getNextPageToken() != null)) {
            decisionTask = getSwf().pollForDecisionTask(request);
            if (decisionTask.getTaskToken() == null) {
                getLog().info("poll timeout");
                if (historyInspector.isEmpty()) {
                    return;
// bail on poll entirely
                }

            } else {
                historyInspector.setWorkflowId(decisionTask.getWorkflowExecution().getWorkflowId());
                historyInspector.setRunId(decisionTask.getWorkflowExecution().getRunId());
                historyInspector.addHistoryEvents(decisionTask.getEvents());
                request.setNextPageToken(decisionTask.getNextPageToken());
            }

        }
        List<Decision> decisions = decide(decisionTask);

        if (decisions.isEmpty()) {
            getLog().info("poll no decisions");
        } else {
            for (Decision decision : decisions) {
                getLog().info("poll decision: " + String.valueOf(decision));
            }
        }

        assert decisionTask != null;
        getSwf().respondDecisionTaskCompleted(createRespondDecisionTaskCompletedRequest(decisionTask.getTaskToken(), decisions));
    }

    public List<Decision> decide(final DecisionTask decisionTask) {
        WorkflowType workflowType = decisionTask.getWorkflowType();
        String key = BasePoller.makeKey(workflowType.getName(), workflowType.getVersion());
        getLog().info("decide " + decisionTask.getWorkflowExecution().getWorkflowId() + " " + key);
        if (!workflows.containsKey(key)) {
            throw new IllegalStateException("Workflow type not registered " + String.valueOf(workflowType));
        }

        List<DecisionStep> steps = workflows.get(key);

        List<Decision> decisions = new ArrayList<>();
        int finishedSteps = 0;
        for (DecisionStep decisionStep : steps) {
            int workflowGroup = historyInspector.getDecisionGroup();
            if (decisionStep.getDecisionGroup() < workflowGroup || decisionStep.isStepFinished()) {
                finishedSteps++;
            } else {
                decisions.addAll(decisionStep.decide());
            }

        }

        if (finishedSteps == steps.size()) {
            String result = calcResult(decisionTask, steps);
            decisions.add(createCompleteWorkflowExecutionDecision(result));
        }

        return decisions;
    }


    /**
     * Calc the final result for the workflow run related to the {@link DecisionTask} parameter.
     * Default implementation returns some run statistics
     *
     * @see CompleteWorkflowExecutionDecisionAttributes#result
     */
    public String calcResult(DecisionTask task, List<DecisionStep> decisionSteps) {
        ObjectNode result = JsonNodeFactory.instance.objectNode();
        ObjectNode context = result.putObject("context")
            .put("deciderId", getId())
            .put("domain", getDomain())
            .put("taskList", getTaskList())
            .put("workflowId", task.getWorkflowExecution().getWorkflowId())
            .put("runId", task.getWorkflowExecution().getRunId())
            .put("name", task.getWorkflowType().getName())
            .put("stepCount", workflows.size());

        ArrayNode steps = context.putArray("steps");
        for (DecisionStep decisionStep : decisionSteps) {
            steps.addObject()
                .put("id", decisionStep.getStepId())
                .put("error", decisionStep.isStepError());
        }
        return SwiftUtil.toJson(result);
    }


    public static Decision createCompleteWorkflowExecutionDecision(String result) {
        return new Decision()
            .withDecisionType(DecisionType.CompleteWorkflowExecution)
            .withCompleteWorkflowExecutionDecisionAttributes(new CompleteWorkflowExecutionDecisionAttributes().withResult(result));
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

    public void setExecutionContext(String executionContext) {
        this.executionContext = executionContext;
    }
}
