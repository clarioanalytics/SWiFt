package com.clario.swift;

import com.amazonaws.services.simpleworkflow.model.*;
import com.clario.swift.action.SwfAction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

/**
 * @author George Coller
 */
public abstract class Workflow {
    protected final String name;
    protected final String version;
    protected final String key;
    protected final SwfHistory swfHistory;
    private final List<String> tagList = new ArrayList<>();

    // Optional fields used for submitting workflow.
    private String domain;
    private String taskList;
    private String description;
    private String executionStartToCloseTimeout;
    private String taskStartToCloseTimeout;
    private String childPolicy = ChildPolicy.TERMINATE.name(); // sensible default

    public Workflow(String name, String version) {
        this.version = version;
        this.name = name;
        this.key = SwiftUtil.makeKey(name, version);
        swfHistory = new SwfHistory();
    }

    public abstract List<SwfAction> getActions();

    public abstract void decide(List<Decision> decisions);

    /**
     * Optional list of activity ids or marker names used to tell poller to stop polling for more history.
     */
    public List<String> getPollingCheckpoints() {
        return Collections.emptyList();
    }

    public String getWorkflowKey() {
        return key;
    }

    public SwfHistory getSwfHistory() { return swfHistory; }

    public boolean isMoreHistoryRequired() {
        for (String checkPoint : getPollingCheckpoints()) {
            if (!swfHistory.actionEvents(checkPoint).isEmpty() || swfHistory.getMarkers().containsKey(checkPoint)) {
                return false;
            }
        }
        return true;
    }

    public void addHistoryEvents(List<HistoryEvent> events) {
        swfHistory.addHistoryEvents(events);
    }

    public List<HistoryEvent> getWorkflowStateErrors() {
        return swfHistory.getWorkflowStateErrors();
    }

    public String getWorkflowInput() {
        return swfHistory.getWorkflowInput();
    }

    /**
     * Called by {@link DecisionPoller} to initialize workflow for current polling.
     */
    public void init(String domain, String taskList) {
        this.domain = domain;
        this.taskList = taskList;
        for (SwfAction action : getActions()) {
            action.setWorkflow(this);
        }
        swfHistory.reset();
    }

    public String getName() { return name; }

    public String getVersion() { return version; }

    public Workflow withDomain(String domain) {
        this.domain = domain;
        return this;
    }

    public String getDomain() {
        return domain;
    }

    public Workflow withTaskList(String taskList) {
        this.taskList = taskList;
        return this;
    }

    public String getTaskList() { return taskList; }

    public Workflow withTags(String... tags) {
        if (tags.length > 5) {
            throw new AssertionError("Expecting a maximum number of 5 workflow tags");
        }
        Collections.addAll(this.tagList, tags);
        return this;
    }

    public Workflow withDescription(String description) {
        this.description = description;
        return this;
    }

    /**
     * The total duration for this workflow execution.
     *
     * @see StartWorkflowExecutionRequest#executionStartToCloseTimeout
     */
    public Workflow withExecutionStartToCloseTimeout(TimeUnit unit, long duration) {
        this.executionStartToCloseTimeout = Long.toString(unit.toSeconds(duration));
        return this;
    }

    /**
     * Set no limit on total duration for this workflow execution.
     *
     * @see StartWorkflowExecutionRequest#executionStartToCloseTimeout
     */
    public Workflow withExecutionStartToCloseTimeoutNone() {
        executionStartToCloseTimeout = "NONE";
        return this;
    }

    /**
     * Specifies the maximum duration of decision tasks for this workflow execution.
     *
     * @see StartWorkflowExecutionRequest#taskStartToCloseTimeout
     */
    public Workflow withTaskStartToCloseTimeout(TimeUnit unit, long duration) {
        this.taskStartToCloseTimeout = Long.toString(unit.toSeconds(duration));
        return this;
    }

    /**
     * Set no limit on total duration of decision tasks for this workflow execution.
     *
     * @see StartWorkflowExecutionRequest#executionStartToCloseTimeout
     */
    public Workflow withTaskStartToCloseTimeoutNone() {
        taskStartToCloseTimeout = "NONE";
        return this;
    }


    /**
     * @see StartWorkflowExecutionRequest#childPolicy
     */
    public Workflow withChildPolicy(ChildPolicy childPolicy) {
        this.childPolicy = childPolicy == null ? null : childPolicy.name();
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
            .withTagList(tagList)
            .withExecutionStartToCloseTimeout(executionStartToCloseTimeout)
            .withTaskStartToCloseTimeout(taskStartToCloseTimeout)
            .withChildPolicy(childPolicy);
    }

    public RegisterWorkflowTypeRequest createRegisterWorkflowTypeRequest() {
        return new RegisterWorkflowTypeRequest()
            .withDomain(domain)
            .withDefaultTaskList(new TaskList().withName(taskList))
            .withName(name)
            .withVersion(version)
            .withDefaultExecutionStartToCloseTimeout(executionStartToCloseTimeout)
            .withDefaultTaskStartToCloseTimeout(taskStartToCloseTimeout)
            .withDefaultChildPolicy(childPolicy)
            .withDescription(description)
            ;
    }


    public final WorkflowType getWorkflowType() {
        return new WorkflowType().withName(name).withVersion(version);
    }

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
        return format("Workflow '%s' ", key);
    }

}
