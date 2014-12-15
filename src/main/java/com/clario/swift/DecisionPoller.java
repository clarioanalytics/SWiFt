package com.clario.swift;

import com.amazonaws.services.simpleworkflow.model.*;
import com.clario.swift.event.Event;
import com.clario.swift.examples.DecisionPollerPool;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static com.amazonaws.services.simpleworkflow.model.EventType.WorkflowExecutionCancelRequested;
import static com.clario.swift.EventList.convert;
import static com.clario.swift.SwiftUtil.*;
import static com.clario.swift.TaskType.WORKFLOW_EXECUTION;
import static com.clario.swift.Workflow.createFailWorkflowExecutionDecision;
import static com.clario.swift.event.EventState.ERROR;
import static java.lang.String.format;


/**
 * Poll for {@link DecisionTask} event on a single domain and task list and ask a registered {@link Workflow} for next decisions.
 * <p/>
 * Implements {@link Runnable} so that multiple instances of this class can be scheduled to handle higher levels of activity tasks.
 *
 * @author George Coller
 * @see BasePoller
 * @see DecisionPollerPool DecisionPollerPool for example usage.
 */
public class DecisionPoller extends BasePoller {
    private final Map<String, Workflow> workflows = new LinkedHashMap<String, Workflow>();
    private final String executionContext;

    /**
     * Construct a decision poller.
     *
     * @param id unique id for poller used for logging and recording in SWF
     * @param domain SWF domain to poll
     * @param taskList SWF taskList to filter on
     * @param executionContext optional value to be sent on each {@link RespondDecisionTaskCompletedRequest}
     */
    public DecisionPoller(String id, String domain, String taskList, String executionContext) {
        super(id, domain, taskList);
        this.executionContext = executionContext;
    }

    /**
     * Register workflows added to this poller on Amazon SWF with this instance's domain and task list.
     * {@link TypeAlreadyExistsException} are ignored making this method idempotent.
     *
     * @see ActivityMethod
     */
    public void registerSwfWorkflows() {
        for (Workflow workflow : workflows.values()) {
            try {
                workflow.withDomain(domain);
                workflow.withTaskList(taskList);
                swf.registerWorkflowType(workflow.createRegisterWorkflowTypeRequest());
                log.info(format("Register workflow succeeded %s", workflow));
            } catch (TypeAlreadyExistsException e) {
                log.info(format("Register workflow already exists %s", workflow));
            } catch (Throwable t) {
                String format = format("Register workflow failed %s", workflow);
                log.error(format);
                throw new IllegalStateException(format, t);
            }
        }
    }

    /**
     * Add {@link Workflow} implementations to the poller
     * mirroring Workflow Types registered on SWF with this poller's domain and task list.
     */
    public void addWorkflows(Workflow... workflows) {
        for (Workflow workflow : workflows) {
            log.info(format("add workflow %s", workflow));
            workflow.withDomain(domain).withTaskList(taskList);
            workflow.assertCanAddToPoller();
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
                if (isLogTimeout()) { log.info("poll timeout"); } // occasionally log a heartbeat for a timed-out poller.
                if (workflow == null) { return; } // return immediately if not currently collecting events for a workflow decision
            } else {
                if (workflow == null) {
                    workflow = lookupWorkflow(decisionTask)
                        .withDomain(domain)
                        .withTaskList(taskList)
                        .withWorkflowId(decisionTask.getWorkflowExecution().getWorkflowId())
                        .withRunId(decisionTask.getWorkflowExecution().getRunId());
                    workflow.init();
                }
                workflow.addEvents(convert(decisionTask.getEvents()));

                if (workflow.isContinuePollingForHistoryEvents()) {
                    request.setNextPageToken(decisionTask.getNextPageToken());
                } else {
                    decisionTask.setNextPageToken(null);
                }
            }
        }

        // Finished loading history for this workflow, now ask it to make the next set of decisions.
        String workflowId = decisionTask.getWorkflowExecution().getWorkflowId();
        String runId = decisionTask.getWorkflowExecution().getRunId();

        List<Decision> decisions = new ArrayList<Decision>();
        EventList currentEvents = workflow.getEvents().selectSinceLastDecision();

        List<Event> workflowErrors = currentEvents.selectTaskType(WORKFLOW_EXECUTION).selectEventState(ERROR);
        if (workflowErrors.isEmpty()) {
            try {
                Event cancelEvent = currentEvents.selectEventType(WorkflowExecutionCancelRequested).getFirst();
                if (cancelEvent != null) {
                    workflow.onCancelRequested(cancelEvent, decisions);
                }

                workflow.decide(decisions);
                if (decisions.isEmpty()) {
                    log.debug("{} no decisions", workflowId, runId);
                } else {
                    if (log.isInfoEnabled()) {
                        for (Decision decision : decisions) {
                            log.info("{} -> {}", workflowId, logNiceDecision(decision));
                        }
                    } else if (log.isDebugEnabled()) {
                        for (Decision decision : decisions) {
                            log.debug("{} -> {}", workflowId, decision);
                        }
                    }
                }
            } catch (Throwable t) {
                String runInfo = format("%s %s", workflowId, runId);
                log.error(runInfo, t);
                decisions.add(createFailWorkflowExecutionDecision(runInfo, t.getMessage(), printStackTrace(t)));
            }
        } else {
            String joinedErrors = join(workflowErrors, "\n");
            Decision failWorkflowExecutionDecision = createFailWorkflowExecutionDecision(format("%s %s", workflowId, runId), "Errors reported", joinedErrors);
            FailWorkflowExecutionDecisionAttributes attributes = failWorkflowExecutionDecision.getFailWorkflowExecutionDecisionAttributes();
            log.error("{}:\n\n{}", attributes.getReason(), attributes.getDetails());
            decisions.add(failWorkflowExecutionDecision);
        }

        try {
            swf.respondDecisionTaskCompleted(createRespondDecisionTaskCompletedRequest(decisionTask.getTaskToken(), decisions));
        } catch (Exception e) {
            log.error(format("%s: %s", workflowId, workflow), e);
        }
    }

    /**
     * Create a nice log message based on the {@link DecisionType} for the given decision.
     */
    public static String logNiceDecision(Decision decision) {
        String decisionType = decision.getDecisionType();
        switch (DecisionType.valueOf(decision.getDecisionType())) {
            case ScheduleActivityTask:
                ScheduleActivityTaskDecisionAttributes a1 = decision.getScheduleActivityTaskDecisionAttributes();
                return String.format("%s['%s' '%s': %s %s]", decisionType, a1.getActivityId(), a1.getActivityType().getName(), a1.getInput(), a1.getControl());
            case CompleteWorkflowExecution:
                CompleteWorkflowExecutionDecisionAttributes a2 = decision.getCompleteWorkflowExecutionDecisionAttributes();
                return String.format("%s[%s]", decisionType, a2.getResult());
            case FailWorkflowExecution:
                FailWorkflowExecutionDecisionAttributes a3 = decision.getFailWorkflowExecutionDecisionAttributes();
                return String.format("%s[%s %s]", decisionType, a3.getReason(), a3.getDetails());
            case CancelWorkflowExecution:
                CancelWorkflowExecutionDecisionAttributes a4 = decision.getCancelWorkflowExecutionDecisionAttributes();
                return String.format("%s[%s]", decisionType, a4.getDetails());
            case ContinueAsNewWorkflowExecution:
                ContinueAsNewWorkflowExecutionDecisionAttributes a5 = decision.getContinueAsNewWorkflowExecutionDecisionAttributes();
                return String.format("%s[%s]", decisionType, a5.getInput());
            case RecordMarker:
                RecordMarkerDecisionAttributes a6 = decision.getRecordMarkerDecisionAttributes();
                return String.format("%s['%s': %s]", decisionType, a6.getMarkerName(), a6.getDetails());
            case StartTimer:
                StartTimerDecisionAttributes a7 = decision.getStartTimerDecisionAttributes();
                return String.format("%s['%s': %s]", decisionType, a7.getTimerId(), a7.getControl());
            case CancelTimer:
                CancelTimerDecisionAttributes a8 = decision.getCancelTimerDecisionAttributes();
                return String.format("%s['%s']", decisionType, a8.getTimerId());
            case SignalExternalWorkflowExecution:
                SignalExternalWorkflowExecutionDecisionAttributes a9 = decision.getSignalExternalWorkflowExecutionDecisionAttributes();
                return String.format("%s['%s' wf='%s' runId='%s': '%s' '%s']", decisionType, a9.getSignalName(), a9.getWorkflowId(), a9.getRunId(), a9.getInput(), a9.getControl());
            case RequestCancelExternalWorkflowExecution:
                RequestCancelExternalWorkflowExecutionDecisionAttributes a10 = decision.getRequestCancelExternalWorkflowExecutionDecisionAttributes();
                return String.format("%s[wf='%s' runId='%s': '%s']", decisionType, a10.getWorkflowId(), a10.getRunId(), a10.getControl());
            case StartChildWorkflowExecution:
                StartChildWorkflowExecutionDecisionAttributes a11 = decision.getStartChildWorkflowExecutionDecisionAttributes();
                return String.format("%s['%s' '%s': '%s' '%s']", decisionType, a11.getWorkflowId(), a11.getWorkflowType().getName(), a11.getInput(), a11.getControl());
            case RequestCancelActivityTask:
                RequestCancelActivityTaskDecisionAttributes a12 = decision.getRequestCancelActivityTaskDecisionAttributes();
                return String.format("%s[%s]", decisionType, a12.getActivityId());
        }
        return null;
    }

    // find the registered workflow related to the current decision task
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
