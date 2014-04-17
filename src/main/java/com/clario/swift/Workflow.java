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
 * @author George Coller
 */
public class Workflow {
    private final Map<String, Task> taskMap;
    private final Map<String, Task> leafTasks;
    private final String name;
    private final String version;
    private final String key;
    private final HistoryInspector historyInspector = new HistoryInspector();

    public Workflow(String name, String version, Map<String, Task> taskMap) {
        this.name = name;
        this.version = version;
        this.taskMap = taskMap;
        this.key = BasePoller.makeKey(name, version);
        for (Task value : taskMap.values()) {
            value.setHistoryInspector(historyInspector);
        }
        this.leafTasks = SwiftUtil.findLeaves(taskMap);
    }

    public String getKey() { return key;}

    public void reset() { historyInspector.reset(); }

    public void addHistoryEvents(List<HistoryEvent> events) { historyInspector.addHistoryEvents(events); }

    public boolean isMoreHistoryRequired() {
        for (Task task : leafTasks.values()) {
            if (task.isMoreHistoryRequired()) {
                return true;
            }
        }
        return false;
    }

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
