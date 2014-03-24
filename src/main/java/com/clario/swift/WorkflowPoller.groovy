package com.clario.swift

import com.amazonaws.services.simpleworkflow.model.*
import groovy.json.JsonOutput

/**
 * Poll for {@link DecisionTask} on a single domain and task list.
 * Note: Single threaded, run multiple instances as {@link Runnable} for higher throughput
 * @author George Coller
 */
public class WorkflowPoller extends BasePoller {
    protected final Map<String, List<DecisionStep>> workflows = [:]
    protected final HistoryInspector historyInspector = new HistoryInspector()

    WorkflowPoller(String id) {
        super(id)
    }

    /**
     * Optional context to be sent on each decision task completed event
     * @see RespondDecisionTaskCompletedRequest#executionContext
     */
    String executionContext

    /**
     * Add a workflow the poller.
     * @param name workflow name
     * @param version workflow version
     * @param decisionSteps decision steps for the workflow
     */
    void addWorkflow(String name, String version, List<DecisionStep> steps) {
        String key = makeKey(name, version)
        log.info "Register activity $key"
        steps.each {
            it.historyInspector = historyInspector
        }
        workflows[key] = steps
    }

    @Override
    protected void poll() {
        PollForDecisionTaskRequest request = createPollForDecisionTaskRequest()
        DecisionTask decisionTask = null
        historyInspector.clear()

        while (historyInspector.decisionGroup < 1 && (decisionTask == null || decisionTask.nextPageToken)) {
            decisionTask = swf.pollForDecisionTask(request)
            if (decisionTask.taskToken == null) {
                log.info "poll timeout"
                if (historyInspector.isEmpty()) {
                    return // bail on poll entirely
                }
            } else {
                historyInspector.workflowId = decisionTask.workflowExecution.workflowId
                historyInspector.runId = decisionTask.workflowExecution.runId
                historyInspector.addHistoryEvents(decisionTask.events)
                request.setNextPageToken(decisionTask.nextPageToken)
            }
        }

        List<Decision> decisions = decide(decisionTask)

        if (decisions) {
            decisions.each {
                log.info "poll decision: $it"
            }
        } else {
            log.info("poll no decisions")
        }
        swf.respondDecisionTaskCompleted(createRespondDecisionTaskCompletedRequest(decisionTask.taskToken, decisions));
    }

    List<Decision> decide(DecisionTask decisionTask) {
        String key = makeKey(decisionTask.workflowType.name, decisionTask.workflowType.version)
        log.info("decide ${decisionTask.workflowExecution.workflowId} $key")
        if (!workflows.containsKey(key)) {
            throw new IllegalStateException("Workflow type not registered ${decisionTask.workflowType}")
        }
        List<DecisionStep> steps = workflows[key]

        List<Decision> decisions = []
        int finishedSteps = 0
        steps.each { DecisionStep decisionStep ->
            int workflowGroup = historyInspector.decisionGroup
            if (decisionStep.decisionGroup < workflowGroup || decisionStep.stepFinished) {
                finishedSteps++
            } else {
                decisions.addAll(decisionStep.decide())
            }
        }
        if (finishedSteps == steps.size()) {
            def result = calcResult(decisionTask, steps)
            decisions.add(createCompleteWorkflowExecutionDecision(result))
        }
        return decisions;
    }

    /**
     * Calc the final result for the workflow run related to the {@link DecisionTask} parameter.
     * Default implementation returns some run statistics
     * @see CompleteWorkflowExecutionDecisionAttributes#result
     */
    String calcResult(DecisionTask task, List<DecisionStep> decisionSteps) {
        def result = [
                context: [
                        deciderId: id,
                        domain: domain,
                        taksList: taskList,
                        workflowId: task.workflowExecution.workflowId,
                        runId: task.workflowExecution.runId,
                        name: task.workflowType.name,
                        stepCount: workflows.size()
                ],
                steps: []
        ]
        decisionSteps.each { DecisionStep step ->
            result.steps.add([id: step.stepId, error: step.stepError])
        }
        return JsonOutput.toJson(result)
    }

    static Decision createCompleteWorkflowExecutionDecision(String result) {
        new Decision()
                .withDecisionType(DecisionType.CompleteWorkflowExecution)
                .withCompleteWorkflowExecutionDecisionAttributes(
                new CompleteWorkflowExecutionDecisionAttributes()
                        .withResult(result))
    }

    RespondDecisionTaskCompletedRequest createRespondDecisionTaskCompletedRequest(String taskToken, List<Decision> decisions) {
        new RespondDecisionTaskCompletedRequest()
                .withDecisions(decisions)
                .withTaskToken(taskToken)
                .withExecutionContext(executionContext)
    }

    PollForDecisionTaskRequest createPollForDecisionTaskRequest() {
        new PollForDecisionTaskRequest()
                .withDomain(domain)
                .withTaskList(new TaskList().withName(taskList))
                .withIdentity(id)
                .withReverseOrder(true)
    }
}