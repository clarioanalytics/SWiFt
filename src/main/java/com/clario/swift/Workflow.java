package com.clario.swift;

import com.amazonaws.services.simpleworkflow.model.*;
import com.clario.swift.action.Action;
import com.clario.swift.event.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.amazonaws.services.simpleworkflow.model.EventType.WorkflowExecutionStarted;
import static com.clario.swift.EventList.byEventType;
import static com.clario.swift.SwiftUtil.*;
import static java.lang.String.format;

/**
 * Contains the decision logic for an SWF Workflow.
 *
 * @author George Coller
 */
public abstract class Workflow {
    private static final Logger log = LoggerFactory.getLogger(Workflow.class);
    private static final int DETAILS_MAX_LENGTH = 32768;
    private static final int REASON_MAX_LENGTH = 256;
    private static final int MARKER_NAME_MAX_LENGTH = 256;
    protected final String name;
    protected final String version;
    protected final String key;
    private final List<String> tags = new ArrayList<>();
    private List<Event> eventList = new ArrayList<>();

    // Optional fields used for submitting workflow.
    private String description;
    private String executionStartToCloseTimeout = SWF_TIMEOUT_YEAR;
    private String taskStartToCloseTimeout = SWF_TIMEOUT_DECISION_DEFAULT;
    private ChildPolicy childPolicy = ChildPolicy.TERMINATE; // sensible default

    // Set by poller
    private String domain;
    private String taskList;
    private String workflowId;
    private String runId;
    private AtomicBoolean canAddToPoller = new AtomicBoolean(true);

    public Workflow(String name, String version) {
        this.name = assertSwfValue(assertMaxLength(name, MAX_NAME_LENGTH));
        this.version = assertSwfValue(assertMaxLength(version, MAX_VERSION_LENGTH));
        this.key = makeKey(name, version);
    }

    /**
     * Add more events to this workflow.
     * Called by {@link DecisionPoller} as it polls for the history for the current workflow to be decided.
     * <p/>
     * NOTE: Assumes the events are in descending order by {@link Event#getEventId()}.
     *
     * @param events events to add
     */
    public void addEvents(List<Event> events) {
        eventList.addAll(events);
    }

    /**
     * Pushes a {@link EventType#TimerStarted} event into the EventList for a given action so that its
     * {@link Action#getState} will equal RETRY instead of SUCCESS OR FAIL for the rest of the current decision pass.
     * Intended side-effect is that <pre>isSuccess</pre> and <pre>isFail</pre> will return false as well.
     */
    public void pushDummyTimerStartedEvent(String actionId) {
        Event event = new Event(
            new HistoryEvent()
                .withEventId(eventList.get(0).getEventId() + 1)
                .withEventTimestamp(new Date())
                .withEventType(EventType.TimerStarted)
                .withTimerStartedEventAttributes(new TimerStartedEventAttributes()
                    .withTimerId(actionId)
                    .withControl("pushDummyTimerStartedEvent")
                ));
        eventList.add(0, event);
    }

    /**
     * Reset instance to prepare for new set of history events.
     */
    public void reset() {
        eventList = new LinkedList<>();
    }

    /**
     * Convenience method that calls {@link #reset} then {@link #addEvents}.
     * <p/>
     * NOTE: Assumes the events are in descending order by {@link Event#getEventId()}.
     */
    public void replaceEvents(List<Event> events) {
        reset();
        addEvents(events);
    }

    /**
     * If available, return the input string given to this workflow when it was initiated on SWF.
     * <p/>
     * This value will not be available if a workflow's {@link Workflow#isContinuePollingForHistoryEvents()} is
     * implemented, which may stop the poller from receiving all of a workflow run's history events.
     *
     * @return the input or null if not available
     */
    public String getWorkflowInput() {
        Event event = getEvents().selectEventType(WorkflowExecutionStarted).getFirst();
        return event == null ? null : event.getInput();
    }

    /**
     * If available return the start date of the workflow when it was initiated on SWF.
     * <p/>
     * This value will not be available if a workflow's {@link Workflow#isContinuePollingForHistoryEvents()} is
     * implemented, which may stop the poller from receiving all of a workflow run's history events.
     *
     * @return the workflow start date or null if not available
     */
    public Date getWorkflowStartDate() {
        Event event = getEvents().select(byEventType(WorkflowExecutionStarted)).getFirst();
        return event == null ? null : event.getEventTimestamp().toDate();
    }

    /**
     * @return {@link EventList} containing all {@link Event} for the current workflow.
     */
    public EventList getEvents() {
        return new EventList(eventList);
    }

    /**
     * Register {@link Action} instances with this workflow so that {@link Action#setWorkflow}
     * will be automatically called with this instance before each {@link #decide}.
     * <p/>
     * Actions that are created dynamically within the {@link #decide} method will have to have
     * {@link Action#setWorkflow} called directly.
     *
     * @see Action#setWorkflow
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
     * @see Action#withCompleteWorkflowOnSuccess
     * @see Action#withNoFailWorkflowOnError
     * @see #createWorkflowExecutionRequest
     * @see #createFailWorkflowExecutionDecision
     */
    public abstract void decide(List<Decision> decisions);

    /**
     * Called if an external process issued a {@link EventType#WorkflowExecutionCancelRequested} for this workflow.
     * By default will simply add a {@link DecisionType#CancelWorkflowExecution} decision.  Subclasses
     * can override this method to gracefully shut down a more complex workflow.
     */
    public void onCancelRequested(Event cancelEvent, List<Decision> decisions) {
        decisions.add(createCancelWorkflowExecutionDecision(cancelEvent.getDetails()));
    }

    /**
     * Called by {@link DecisionPoller} to initialize workflow for a new decision task.
     */
    public void init() {
        eventList = new LinkedList<>();
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
     * The decision poller calls this method after each call to {@link #addEvents}
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
    public Workflow withTags(Collection<String> tags) {
        for (String tag : tags) {
            this.tags.add(assertMaxLength(tag, MAX_NAME_LENGTH));
        }
        if (this.tags.size() > MAX_NUMBER_TAGS) {
            throw new AssertionError(format("More than %d tags not allowed, received: %s", MAX_NUMBER_TAGS, join(this.tags, ",")));
        }
        return this;
    }

    /** Optional tags submitted with workflow */
    public Workflow withTags(String... tags) {
        return withTags(Arrays.asList(tags));
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
     * Pass null unit or duration &lt;= 0 for default timeout period of 365 days.
     * Default is 365 days.
     *
     * @see StartWorkflowExecutionRequest#executionStartToCloseTimeout
     */
    public Workflow withExecutionStartToCloseTimeout(TimeUnit unit, long duration) {
        executionStartToCloseTimeout = calcTimeoutOrYear(unit, duration);
        return this;
    }

    public String getExecutionStartToCloseTimeout() { return executionStartToCloseTimeout; }

    /**
     * Specifies the maximum duration of <b>decision</b> tasks for this workflow execution.
     * Pass null unit or duration &lt;= 0 for a timeout of NONE.
     * <p/>
     * Defaults to one minute, which should be plenty of time deciders that don't need to
     * connect to external services to make the next decision.
     *
     * @see StartWorkflowExecutionRequest#taskStartToCloseTimeout
     */
    public Workflow withTaskStartToCloseTimeout(TimeUnit unit, long duration) {
        this.taskStartToCloseTimeout = calcTimeoutOrNone(unit, duration);
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

    public static Decision createCancelWorkflowExecutionDecision(String result) {
        return new Decision()
            .withDecisionType(DecisionType.CancelWorkflowExecution)
            .withCancelWorkflowExecutionDecisionAttributes(
                new CancelWorkflowExecutionDecisionAttributes()
                    .withDetails(trimToMaxLength(result, DETAILS_MAX_LENGTH)));
    }

    /**
     * Create a fail workflow reason by combining a target name and message.
     *
     * @param target target name, optional, usually the <code>toString()</code> of the object that caused an error.
     * @param message error message
     *
     * @return message if target is null, otherwise target and message combined into a single string
     */
    public static String createFailReasonString(String target, String message) {
        String fail = target == null ? message : format("%s:\n%s", target, message);
        return trimToMaxLength(fail, REASON_MAX_LENGTH);
    }

    public static Decision createFailWorkflowExecutionDecision(String target, String reason, String details) {
        return new Decision()
            .withDecisionType(DecisionType.FailWorkflowExecution)
            .withFailWorkflowExecutionDecisionAttributes(
                new FailWorkflowExecutionDecisionAttributes()
                    .withReason(createFailReasonString(target, reason))
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


    void assertCanAddToPoller() {
        if (!canAddToPoller.compareAndSet(true, false)) {
            String msg = format("Attempt to add same instance of workflow %s to multiple decision pollers", this);
            log.error(msg);
            throw new IllegalStateException(msg);
        }
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
        return key;
    }

}
