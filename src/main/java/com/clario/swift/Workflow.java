package com.clario.swift;

import com.amazonaws.services.simpleworkflow.model.*;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.clario.swift.SwiftUtil.join;
import static java.lang.String.format;

/**
 * Maps to a registered Simple Workflow 'workflow' and contains a graph of {@link Task}
 * to be decided.
 *
 * @author George Coller
 */
public class Workflow {
    private final Map<String, Task> taskMap;
    private final Map<String, Task> leafTasks;
    private final String name;
    private final String version;
    private final String key;
    private final HistoryInspector historyInspector = new HistoryInspector();

    /**
     * Construct a workflow mapping to a registered SWF workflow.
     *
     * @param name registered name
     * @param version registered version
     * @param tasks graph of tasks to be decided
     */
    public Workflow(String name, String version, Map<String, Task> tasks) {
        this.name = name;
        this.version = version;
        this.taskMap = tasks;
        this.key = BasePoller.makeKey(name, version);
        for (Task value : tasks.values()) {
            value.setHistoryInspector(historyInspector);
        }
        this.leafTasks = Vertex.findLeaves(tasks);
    }

    /**
     * Unique id for this workflow, combination of name and version.
     */
    public String getKey() { return key;}

    /**
     * Reset the workflow state so it can be reused on the next polling.
     */
    public void reset() { historyInspector.reset(); }

    /**
     * Add more {@link HistoryEvent} instances to the workflow.
     *
     * @see #isMoreHistoryRequired()
     */
    public void addHistoryEvents(List<HistoryEvent> events) {
        historyInspector.addHistoryEvents(events);
    }

    /**
     * Are more history events required before this workflow instance can make
     * the next round of decisions.
     * <p/>
     * Since Amazon SWF only returns 1000 events at a time, for more complex workflows it
     * is a performance gain to let the {@link WorkflowPoller} know if it can stop polling for more
     * {@link HistoryEvent} records.
     *
     * @see Task#isMoreHistoryRequired()
     */
    public boolean isMoreHistoryRequired() {
        for (Task task : leafTasks.values()) {
            if (task.isMoreHistoryRequired()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Make decisions given the current state of the workflow.
     * <p/>
     * If the workflow is finished (all tasks are in a finish state) then a final
     * workflow complete decision will be returned.
     *
     * @param workflowId id given to this run of the workflow.
     *
     * @return list of zero or more decisions made
     */
    public List<Decision> decide(String workflowId) {
        List<Decision> decisions = new ArrayList<>();
        int finishedTasks = 0;
        int errorTasks = 0;
        for (Task task : taskMap.values()) {
            TaskState taskState = task.getState();
            if (taskState.isFinished()) {
                finishedTasks++;
                if (TaskState.finish_error == taskState) {
                    errorTasks++;
                }
            } else if (TaskState.ready_to_decide == taskState) {
                decisions.addAll(task.decide());
            }
        }

        if (finishedTasks == taskMap.size()) {
            String result = calcWorkflowCompletionResult(workflowId);
            if (errorTasks > 0) {
                decisions.add(createFailWorkflowExecutionDecision(result, null));
            } else {
                decisions.add(createCompleteWorkflowExecutionDecision(result));
            }
        }

        return decisions;
    }

    /**
     * Calc the final result for the workflow run.
     *
     * @see CompleteWorkflowExecutionDecisionAttributes#result
     */
    protected String calcWorkflowCompletionResult(String workflowId) {
        ObjectNode result = JsonNodeFactory.instance.objectNode();
        ObjectNode context = result.putObject("context")
            .put("name", name)
            .put("version", version)
            .put("workflowId", workflowId);

        ArrayNode taskArray = context.putArray("tasks");
        for (Task task : taskMap.values()) {
            taskArray.addObject()
                .put("id", task.getId())
                .put("error", TaskState.finish_error == task.getState());
        }
        return SwiftUtil.toJson(result);
    }

    public static Decision createCompleteWorkflowExecutionDecision(String result) {
        return new Decision()
            .withDecisionType(DecisionType.CompleteWorkflowExecution)
            .withCompleteWorkflowExecutionDecisionAttributes(
                new CompleteWorkflowExecutionDecisionAttributes().withResult(result));
    }

    public static Decision createFailWorkflowExecutionDecision(String reason, String details) {
        return new Decision()
            .withDecisionType(DecisionType.FailWorkflowExecution)
            .withFailWorkflowExecutionDecisionAttributes(
                new FailWorkflowExecutionDecisionAttributes()
                    .withReason(reason)
                    .withDetails(details)
            );
    }

    @Override
    public boolean equals(Object o) {
        return this == o || !(o == null || getClass() != o.getClass()) && key.equals(((Workflow) o).key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }

    public String toString() {
        StringBuilder b = new StringBuilder(taskMap.size() + 50);
        b.append("Workflow '")
            .append(name)
            .append("' '")
            .append(version)
            .append("'\n");
        for (Task task : taskMap.values()) {
            b.append(task.getId());
            if (task instanceof Activity) {
                Activity activity = (Activity) task;
                b.append(format(" '%s' '%s'", activity.getName(), activity.getVersion()));
            }
            if (!task.getParents().isEmpty()) {
                List<String> parents = new ArrayList<>();
                for (Task child : task.getParents()) {
                    parents.add(child.getId());
                }
                b.append(format(" parents(%s)", join(parents, ", ")));
            }
            b.append('\n');
        }
        return b.toString();
    }
}
