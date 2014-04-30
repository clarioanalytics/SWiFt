package com.clario.swift;

import com.amazonaws.services.simpleworkflow.model.*;
import com.clario.swift.action.Action;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.clario.swift.SwiftUtil.*;
import static java.lang.String.format;

/**
 * Implementation of a registered SWF Workflow.
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

    private final Set<String> checkpoints = new LinkedHashSet<>();

    public Workflow(String name, String version) {
        this.version = version;
        this.name = name;
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
     * should be returned to indicate the workflow is complete.
     *
     * @see #createWorkflowExecutionRequest
     * @see #createFailWorkflowExecutionDecision
     */
    public abstract void decide(List<Decision> decisions);

    /**
     * Override to add one or more strings that will match to either an {@link Action#actionId} or recorded SWF Marker name to indicate
     * to the {@link DecisionPoller} that it can stop polling for more history for a given {@link DecisionTask}.
     * <p/>
     * Amazon limits the number of {@link HistoryEvent} returned when polling for the next decision task to 1000 at a time.  For
     * complex workflows that generate thousands of events (note even a simple workflow can create several dozen events) it may
     * be important to create polling checkpoints to reduce the load on {@link DecisionPoller} instances.
     *
     * @see #getCurrentCheckpoint()
     */
    public void withCheckpoints(String... checkpoints) {
        Collections.addAll(this.checkpoints, checkpoints);
    }

    /**
     * Return the most recent checkpoint value found in the added {@link HistoryEvent} for the current decision task;
     *
     * @return the checkpoint or null if none found;
     * @see #withCheckpoints
     */
    public String getCurrentCheckpoint() {
        for (String checkpoint : checkpoints) {
            if (!workflowHistory.filterEvents(checkpoint).isEmpty() || workflowHistory.getMarkers().containsKey(checkpoint)) {
                return checkpoint;
            }
        }
        return null;
    }

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
        this.domain = domain;
        return this;
    }

    /** Domain-unique workflow execution identifier * */
    public Workflow withWorkflowId(String workflowId) {
        this.workflowId = workflowId;
        return this;
    }

    /** Domain-unique workflow execution identifier * */
    public String getWorkflowId() { return workflowId; }

    /** SWF generated unique run id for a specific workflow execution. */
    public Workflow withRunId(String runId) {
        this.runId = runId;
        return this;
    }

    /** SWF generated unique run id for a specific workflow execution. */
    public String getRunId() { return runId; }

    /** SWF task list this workflow is/will be executed under */
    public Workflow withTaskList(String taskList) {
        this.taskList = taskList;
        return this;
    }

    /** SWF task list this workflow is/will be executed under */
    public String getTaskList() { return taskList; }


    /** Optional tags submitted with workflow */
    public Workflow withTags(String... tags) {
        Collections.addAll(this.tags, tags);
        return this;
    }

    /** Optional description to register with workflow */
    public Workflow withDescription(String description) {
        this.description = description;
        return this;
    }

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

    /**
     * defaults to TERMINATE
     *
     * @see StartWorkflowExecutionRequest#childPolicy
     */
    public Workflow withChildPolicy(ChildPolicy childPolicy) {
        this.childPolicy = childPolicy;
        return this;
    }


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
        return this == o || !(o == null || getClass() != o.getClass()) && key.equals(((Workflow) o).key);
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
