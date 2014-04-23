package com.clario.swift.action;

import com.amazonaws.services.redshift.model.UnsupportedOptionException;
import com.amazonaws.services.simpleworkflow.model.*;
import com.clario.swift.SwfHistoryEvent;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

/**
 * Start a child workflow from the current workflow.
 *
 * @author George Coller
 */
public class SwfStartChildWorkflow extends SwfAction {
    private String name;
    private String version;
    private String taskList;
    private String input;
    private String executionStartToCloseTimeout;
    private String taskStartToCloseTimeout;
    private String childPolicy = ChildPolicy.TERMINATE.name(); // sensible default
    private final List<String> tagList = new ArrayList<>();

    public SwfStartChildWorkflow(String id) {
        super(id);
    }

    public SwfStartChildWorkflow withName(String name) {
        this.name = name;
        return this;
    }

    public SwfStartChildWorkflow withVersion(String version) {
        this.version = version;
        return this;
    }

    public SwfStartChildWorkflow withTaskList(String taskList) {
        this.taskList = taskList;
        return this;
    }

    public SwfStartChildWorkflow withTags(String... tags) {
        if (tags.length > 5) {
            throw new AssertionError("Expecting a maximum number of 5 workflow tags");
        }
        Collections.addAll(this.tagList, tags);
        return this;
    }

    public SwfStartChildWorkflow withInput(String input) {
        this.input = input;
        return this;
    }

    /**
     * The total duration for this workflow execution.
     *
     * @see StartWorkflowExecutionRequest#executionStartToCloseTimeout
     */
    public SwfStartChildWorkflow withExecutionStartToCloseTimeout(TimeUnit unit, long duration) {
        this.executionStartToCloseTimeout = Long.toString(unit.toSeconds(duration));
        return this;
    }

    /**
     * Set no limit on total duration for this workflow execution.
     *
     * @see StartWorkflowExecutionRequest#executionStartToCloseTimeout
     */
    public SwfStartChildWorkflow withExecutionStartToCloseTimeoutNone() {
        executionStartToCloseTimeout = "NONE";
        return this;
    }

    /**
     * Specifies the maximum duration of decision tasks for this workflow execution.
     *
     * @see StartWorkflowExecutionRequest#taskStartToCloseTimeout
     */
    public SwfStartChildWorkflow withTaskStartToCloseTimeout(TimeUnit unit, long duration) {
        this.taskStartToCloseTimeout = Long.toString(unit.toSeconds(duration));
        return this;
    }

    /**
     * Set no limit on total duration of decision tasks for this workflow execution.
     *
     * @see StartWorkflowExecutionRequest#executionStartToCloseTimeout
     */
    public SwfStartChildWorkflow withTaskStartToCloseTimeoutNone() {
        taskStartToCloseTimeout = "NONE";
        return this;
    }


    /**
     * @see StartWorkflowExecutionRequest#childPolicy
     */
    public SwfStartChildWorkflow withChildPolicy(ChildPolicy childPolicy) {
        this.childPolicy = childPolicy == null ? null : childPolicy.name();
        return this;
    }

    @Override
    public Decision createDecision() {
        return new Decision()
            .withDecisionType(DecisionType.StartChildWorkflowExecution)
            .withStartChildWorkflowExecutionDecisionAttributes(new StartChildWorkflowExecutionDecisionAttributes()
                    .withWorkflowId(id)
                    .withWorkflowType(new WorkflowType().withName(name).withVersion((version)))
                    .withTaskList(new TaskList().withName(taskList))
                    .withInput(input)
                    .withExecutionStartToCloseTimeout(executionStartToCloseTimeout)
                    .withTaskStartToCloseTimeout(taskStartToCloseTimeout)
                    .withChildPolicy(childPolicy)
                    .withTagList(tagList)
            );
    }

    public String getChildRunId() {
        for (SwfHistoryEvent event : getActionHistoryEvents()) {
            if (event.getType() == EventType.ChildWorkflowExecutionStarted) {
                return event.getResult();
            }
        }
        throw new UnsupportedOptionException(format("RunId not available %s %s", this, getState()));
    }

    public String getOutput() {
        if (ActionState.finish_ok != getState()) {
            throw new UnsupportedOptionException(format("Result not available %s %s", this, getState()));
        }
        return getCurrentActionHistoryEvent().getResult();
    }
}
