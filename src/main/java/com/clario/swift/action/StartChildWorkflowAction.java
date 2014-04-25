package com.clario.swift.action;

import com.amazonaws.services.redshift.model.UnsupportedOptionException;
import com.amazonaws.services.simpleworkflow.model.*;
import com.clario.swift.ActionHistoryEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.clario.swift.SwiftUtil.calcTimeoutString;
import static com.clario.swift.SwiftUtil.makeKey;
import static java.lang.String.format;

/**
 * Start a child workflow from the current workflow.
 *
 * @author George Coller
 * @see StartChildWorkflowExecutionDecisionAttributes
 */
public class StartChildWorkflowAction extends Action<StartChildWorkflowAction> {
    private String name;
    private String version;
    private String taskList;
    private String input;
    private String executionStartToCloseTimeout;
    private String taskStartToCloseTimeout;
    private String childPolicy = ChildPolicy.TERMINATE.name(); // sensible default
    private final List<String> tagList = new ArrayList<>();

    public StartChildWorkflowAction(String actionId) {
        super(actionId);
    }

    /**
     * Registered SWF Workflows are uniquely identified by the combination of name and version
     * so both are required.
     */
    public StartChildWorkflowAction withNameVersion(String name, String version) {
        this.name = name;
        this.version = version;
        return this;
    }

    /**
     * Set the taskList for the child workflow.
     * If null, defaults to the calling workflow taskList.
     *
     * @see com.clario.swift.Workflow#getTaskList()
     * @see StartChildWorkflowExecutionDecisionAttributes#taskList
     */
    public StartChildWorkflowAction withTaskList(String taskList) {
        this.taskList = taskList;
        return this;
    }

    /**
     * @see StartChildWorkflowExecutionDecisionAttributes#tagList
     */
    public StartChildWorkflowAction withTags(String... tags) {
        Collections.addAll(this.tagList, tags);
        return this;
    }

    /**
     * @see StartChildWorkflowExecutionDecisionAttributes#input
     */
    public StartChildWorkflowAction withInput(String input) {
        this.input = input;
        return this;
    }

    /**
     * Override the child workflow default start to close timeout.
     * Pass null unit or duration &lt;= 0 for a timeout of NONE.
     *
     * @see StartWorkflowExecutionRequest#executionStartToCloseTimeout
     */
    public StartChildWorkflowAction withExecutionStartToCloseTimeout(TimeUnit unit, long duration) {
        this.executionStartToCloseTimeout = calcTimeoutString(unit, duration);
        return this;
    }

    /**
     * Override the child workflow task default start to close timeout.
     * Pass null unit or duration &lt;= 0 for a timeout of NONE.
     *
     * @see StartWorkflowExecutionRequest#taskStartToCloseTimeout
     */
    public StartChildWorkflowAction withTaskStartToCloseTimeout(TimeUnit unit, long duration) {
        this.taskStartToCloseTimeout = calcTimeoutString(unit, duration);
        return this;
    }

    /**
     * Override child workflow default child policy.
     * Set the child policy, defaults to {@link ChildPolicy#TERMINATE}.
     *
     * @see StartWorkflowExecutionRequest#childPolicy
     */
    public StartChildWorkflowAction withChildPolicy(ChildPolicy childPolicy) {
        this.childPolicy = childPolicy == null ? null : childPolicy.name();
        return this;
    }

    /**
     * @return decision of type {@link DecisionType#StartChildWorkflowExecution}
     */
    @Override
    public Decision createInitiateActivityDecision() {
        return new Decision()
            .withDecisionType(DecisionType.StartChildWorkflowExecution)
            .withStartChildWorkflowExecutionDecisionAttributes(new StartChildWorkflowExecutionDecisionAttributes()
                    .withWorkflowId(getActionId())
                    .withWorkflowType(new WorkflowType().withName(name).withVersion((version)))
                    .withTaskList(new TaskList().withName(taskList == null ? getWorkflow().getTaskList() : taskList))
                    .withInput(input)
                    .withExecutionStartToCloseTimeout(executionStartToCloseTimeout)
                    .withTaskStartToCloseTimeout(taskStartToCloseTimeout)
                    .withChildPolicy(childPolicy)
                    .withTagList(tagList)
            );
    }

    /**
     * Get the run identifier of the child workflow.
     * Clients should ensure that the child workflow has been started in a prior decision task before calling this method.
     *
     * @throws UnsupportedOperationException if child runId is not available
     * @see #getState()
     */
    public String getChildRunId() {
        for (ActionHistoryEvent event : getHistoryEvents()) {
            if (event.getType() == EventType.ChildWorkflowExecutionStarted) {
                return event.getHistoryEvent().getChildWorkflowExecutionStartedEventAttributes().getWorkflowExecution().getRunId();
            }
        }
        throw new UnsupportedOptionException(format("RunId not available %s %s", this, getState()));
    }

    /**
     * Get the output of the child workflow.
     * Clients should ensure that the child workflow has finished successfully before calling this method.
     *
     * @throws UnsupportedOperationException if child output is not available
     * @see #getState()
     */
    public String getOutput() {
        if (!isSuccess()) {
            throw new UnsupportedOptionException(format("Result not available %s %s", this, getState()));
        }
        return getCurrentHistoryEvent().getResult();
    }

    @Override
    public String toString() {
        return format("%s %s %s", getClass().getSimpleName(), getActionId(), makeKey(name, version));
    }
}
