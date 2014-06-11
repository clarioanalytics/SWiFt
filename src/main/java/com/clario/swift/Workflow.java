package com.clario.swift;

import com.amazonaws.services.simpleworkflow.model.*;
import com.clario.swift.action.Action;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.clario.swift.SwiftUtil.*;
import static java.lang.String.format;

/**
 * Contains the decision logic for an SWF Workflow.
 *
 * @author George Coller
 */
public abstract class Workflow {
    private static final int DETAILS_MAX_LENGTH = 32768;
    private static final int REASON_MAX_LENGTH = 256;
    private static final int MARKER_NAME_MAX_LENGTH = 256;
    protected final String name;
    protected final String version;
    protected final String key;
    protected final WorkflowHistory workflowHistory;
    private final List<String> tags = new ArrayList<>();

    // Optional fields used for submitting workflow.
    private String description;
    private String executionStartToCloseTimeout = null;
    private String taskStartToCloseTimeout = "NONE";
    private ChildPolicy childPolicy = ChildPolicy.TERMINATE; // sensible default

    // Set by poller
    private String domain;
    private String taskList;
    private String workflowId;
    private String runId;

    public Workflow(String name, String version) {
        this.name = assertSwfValue(assertMaxLength(name, MAX_NAME_LENGTH));
        this.version = assertSwfValue(assertMaxLength(version, MAX_VERSION_LENGTH));
        this.key = makeKey(name, version);
        workflowHistory = new WorkflowHistory();
    }

    /**
     * Called by subclass add a reference of this instance to each action used on the workflow.
     * Actions require a reference to their enclosing workflow to access the current decision task state.
     */
    protected void addActions(Action... actions) {
        for (Action action : actions) {
            action.setWorkflow(this);
        }
    }

    /**
     * Subclasses add zero or more decisions to the parameter during a decision task.
     * A final {@link DecisionType#CompleteWorkflowExecution} or  {@link DecisionType#FailWorkflowExecution}
     * should be returned to indicate the workflow is complete. These decisions can be added by
     * {@link Action} instances automatically given their final state.
     * <p/>
     * An {@link DecisionType#FailWorkflowExecution} decision will be decided by the {@link DecisionPoller} if an unhandled exception is thrown
     * by this method.
     *
     * @see Action#decide
     * @see Action#withCompleteWorkflowOnSuccess()
     * @see Action#withNoFailWorkflowOnError()
     * @see #createWorkflowExecutionRequest
     * @see #createFailWorkflowExecutionDecision
     */
    public abstract void decide(List<Decision> decisions);

    /**
     * Called by {@link DecisionPoller} to initialize workflow for a new decision task.
     */
    public void init() {
        workflowHistory.reset();
    }


    public String getName() { return name; }

    public String getVersion() { return version; }

    /**
     * Workflow name and version glued together to make a key.
     *
     * @see SwiftUtil#makeKey
     */
    public String getKey() { return key; }

    /**
     * @return this instance's history container
     */
    public WorkflowHistory getWorkflowHistory() { return workflowHistory; }


    /**
     * Add history events for current SWF decision task.
     *
     * @see WorkflowHistory#addHistoryEvents
     */
    public void addHistoryEvents(List<HistoryEvent> events) {
        workflowHistory.addHistoryEvents(events);
    }

    /**
     * The decision poller calls this method after each call to {@link #addHistoryEvents}
     * to see if it should continue polling for more history or if this workflow
     * currently has enough history to make its next set of decisions.
     * <p/>
     * Amazon SWF only returns 1000 history events on each poll.
     * For workflows that generate thousands of events this method can
     * be overridden to improve performance.
     * <p/>
     * The default implementation assumes all history events are required
     * before calling {@link #decide}
     *
     * @return true if more history required, else false.
     */
    public boolean isContinuePollingForHistoryEvents() {
        return true;
    }

    /**
     * Get any error events recorded for current SWF decision task.
     *
     * @see WorkflowHistory#getErrorEvents
     */
    public List<HistoryEvent> getErrorEvents() {
        return workflowHistory.getErrorEvents();
    }

    /**
     * Get input provided when current decision task's workflow was submitted.
     *
     * @see WorkflowHistory#getWorkflowInput()
     */
    public String getWorkflowInput() {
        return workflowHistory.getWorkflowInput();
    }

    /** SWF domain */
    public Workflow withDomain(String domain) {
        this.domain = assertSwfValue(assertMaxLength(domain, MAX_NAME_LENGTH));
        return this;
    }

    public String getDomain() { return domain; }

    /** Domain-unique workflow execution identifier * */
    public Workflow withWorkflowId(String workflowId) {
        this.workflowId = assertSwfValue(assertMaxLength(workflowId, MAX_ID_LENGTH));
        return this;
    }

    /** Domain-unique workflow execution identifier * */
    public String getWorkflowId() { return workflowId; }

    /** SWF generated unique run id for a specific workflow execution. */
    public Workflow withRunId(String runId) {
        this.runId = assertMaxLength(runId, MAX_RUN_ID_LENGTH);
        return this;
    }

    /** SWF generated unique run id for a specific workflow execution. */
    public String getRunId() { return runId; }

    /** SWF task list this workflow is/will be executed under */
    public Workflow withTaskList(String taskList) {
        this.taskList = assertSwfValue(assertMaxLength(taskList, MAX_NAME_LENGTH));
        return this;
    }

    /** SWF task list this workflow is/will be executed under */
    public String getTaskList() { return taskList; }


    /** Optional tags submitted with workflow */
    public Workflow withTags(String... tags) {
        for (String tag : tags) {
            this.tags.add(assertMaxLength(tag, MAX_NAME_LENGTH));
        }
        if (this.tags.size() > MAX_NUMBER_TAGS) {
            throw new AssertionError(String.format("More than %d tags not allowed, received: %s", MAX_NUMBER_TAGS, join(this.tags, ",")));
        }
        return this;
    }

    public List<String> getTags() { return tags; }

    /** Optional description to register with workflow */
    public Workflow withDescription(String description) {
        this.description = assertMaxLength(description, MAX_DESCRIPTION_LENGTH);
        return this;
    }

    public String getDescription() { return description; }

    /**
     * The total duration for this workflow execution.
     * Pass null unit or duration &lt;= 0 for default timeout period.
     * <p/>
     * Note: Unlike other timeouts a value of NONE is not allowed.
     *
     * @see StartWorkflowExecutionRequest#executionStartToCloseTimeout
     */
    public Workflow withExecutionStartToCloseTimeout(TimeUnit unit, long duration) {
        String timeoutString = calcTimeoutString(unit, duration);
        executionStartToCloseTimeout = TIMEOUT_NONE.equals(timeoutString) ? null : timeoutString;
        return this;
    }

    public String getExecutionStartToCloseTimeout() { return executionStartToCloseTimeout; }

    /**
     * Specifies the maximum duration of decision tasks for this workflow execution.
     * Pass null unit or duration &lt;= 0 for a timeout of NONE.
     * defaults to NONE.
     *
     * @see StartWorkflowExecutionRequest#taskStartToCloseTimeout
     */
    public Workflow withTaskStartToCloseTimeout(TimeUnit unit, long duration) {
        this.taskStartToCloseTimeout = calcTimeoutString(unit, duration);
        return this;
    }

    public String getTaskStartToCloseTimeout() { return taskStartToCloseTimeout; }

    /**
     * defaults to TERMINATE
     *
     * @see StartWorkflowExecutionRequest#childPolicy
     */
    public Workflow withChildPolicy(ChildPolicy childPolicy) {
        this.childPolicy = childPolicy;
        return this;
    }

    public ChildPolicy getChildPolicy() { return childPolicy; }

    public StartWorkflowExecutionRequest createWorkflowExecutionRequest(String workflowId, String input) {
        return new StartWorkflowExecutionRequest()
            .withWorkflowId(workflowId)
            .withDomain(domain)
            .withTaskList(new TaskList()
                .withName(taskList))
            .withWorkflowType(new WorkflowType()
                .withName(name)
                .withVersion(version))
            .withInput(input)
            .withTagList(tags)
            .withExecutionStartToCloseTimeout(executionStartToCloseTimeout)
            .withTaskStartToCloseTimeout(taskStartToCloseTimeout)
            .withChildPolicy(childPolicy == null ? null : childPolicy.name());
    }

    public RegisterWorkflowTypeRequest createRegisterWorkflowTypeRequest() {
        return new RegisterWorkflowTypeRequest()
            .withDomain(domain)
            .withDefaultTaskList(new TaskList().withName(taskList))
            .withName(name)
            .withVersion(version)
            .withDefaultExecutionStartToCloseTimeout(executionStartToCloseTimeout)
            .withDefaultTaskStartToCloseTimeout(taskStartToCloseTimeout)
            .withDefaultChildPolicy(childPolicy == null ? null : childPolicy.name())
            .withDescription(description)
            ;
    }

    public static Decision createCompleteWorkflowExecutionDecision(String result) {
        return new Decision()
            .withDecisionType(DecisionType.CompleteWorkflowExecution)
            .withCompleteWorkflowExecutionDecisionAttributes(
                new CompleteWorkflowExecutionDecisionAttributes()
                    .withResult(trimToMaxLength(result, DETAILS_MAX_LENGTH))
            );
    }

    public static Decision createFailWorkflowExecutionDecision(String reason, String details) {
        return new Decision()
            .withDecisionType(DecisionType.FailWorkflowExecution)
            .withFailWorkflowExecutionDecisionAttributes(
                new FailWorkflowExecutionDecisionAttributes()
                    .withReason(trimToMaxLength(reason, REASON_MAX_LENGTH))
                    .withDetails(trimToMaxLength(details, DETAILS_MAX_LENGTH))
            );
    }

    public static Decision createRecordMarkerDecision(String name, String details) {
        return new Decision()
            .withDecisionType(DecisionType.RecordMarker)
            .withRecordMarkerDecisionAttributes(new RecordMarkerDecisionAttributes()
                    .withMarkerName(trimToMaxLength(name, MARKER_NAME_MAX_LENGTH))
                    .withDetails(trimToMaxLength(details, DETAILS_MAX_LENGTH))
            );
    }

    /**
     * Two workflows are considered equal if they have the same name and version.
     */
    @Override
    public boolean equals(Object o) {
        return o == this || (o != null && o instanceof Workflow && key.equals(((Workflow) o).key));
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    @Override
    public String toString() {
        return format("Workflow %s ", key);
    }

}
