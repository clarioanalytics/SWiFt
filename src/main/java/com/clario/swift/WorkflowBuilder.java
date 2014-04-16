package com.clario.swift;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.clario.swift.SwiftUtil.join;
import static java.lang.String.format;

/**
 * Builder to create a workflow, which is a simple directed graph of {@link Task}.
 *
 * @author George Coller
 */
public class WorkflowBuilder {
    public static final Logger log = LoggerFactory.getLogger(WorkflowBuilder.class);

    private final Map<String, Task> taskMap = new LinkedHashMap<>();
    private final List<Task> withGroup = new ArrayList<>();
    private int checkpointCounter = 0;

    /**
     * Add a {@link Task} to the workflow.
     *
     * @param task task to add
     *
     * @return this instance for chaining
     */
    public WorkflowBuilder add(Task task) {
        String id = task.getId();
        if (taskMap.containsKey(id)) {
            throw new IllegalArgumentException(format("Task '%s' already added", id));
        }
        taskMap.put(id, task);
        task.setCheckpoint(checkpointCounter);
        withGroup.clear();
        withGroup.add(task);
        return this;
    }

    /**
     * Add an {@link Activity} task.
     *
     * @param id task identifier, must be unique per workflow
     * @param name name of registered activity in SWF
     * @param version version of registered activity in SWF
     *
     * @return this instance for chaining
     */
    public WorkflowBuilder activity(String id, String name, String version) {
        return add(new Activity(id, name, version));
    }

    /**
     * Select a set of tasks as parents.
     *
     * @param regExp regular expression to match parent task ids
     *
     * @return this instance for chaining
     */
    public WorkflowBuilder addParents(String regExp) {
        assertWithGroup();
        List<Task> taskList = findTasks(regExp);
        for (Task parent : taskList) {
            for (Task task : withGroup) {
                if (parent.getCheckpoint() > task.getCheckpoint()) {
                    throw new IllegalStateException(String.format("Cannot add parent task '%s' with checkpoint after task '%s'", parent.getId(), task.getId()));
                }
                if (parent.getCheckpoint() < task.getCheckpoint() - 1) {
                    throw new IllegalStateException(String.format("Cannot add parent task '%s' to task '%s' across two checkpoints", parent.getId(), task.getId()));
                }
                task.addParents(parent);
            }
        }
        return this;
    }

    /**
     * Apply retry settings to tasks.
     *
     * @param times number of times to retry task before failing
     * @param retryWaitInMillis exponential backoff algorithm initial wait time in milliseconds
     *
     * @return this instance for chaining
     */
    public WorkflowBuilder retry(int times, long retryWaitInMillis) {
        assertWithGroup();
        for (Task task : withGroup) {
            task.addRetry(times, retryWaitInMillis);
        }
        return this;
    }

    /**
     * Create a {@link Checkpoint} task to indicate that all prior tasks, or prior tasks since the last checkpoint,
     * will need to complete before continuing.
     *
     * @return this instance for chaining
     * @see Checkpoint
     */
    public WorkflowBuilder checkpoint() {
        checkpointCounter++;
        return this;
    }


    /**
     * Select a set of tasks to modify as a group.
     * <p/>
     * Example:
     * <pre>
     *     builder.withTasks("task.*").retry(3, 5000)
     * </pre>
     *
     * @param regExprs one or more regular expressions used match task ids.
     *
     * @return this instance for chaining
     */
    public WorkflowBuilder withTasks(String... regExprs) {
        withGroup.clear();
        for (String regExp : regExprs) {
            withGroup.addAll(findTasks(regExp));
        }
        assertWithGroup();
        return this;
    }


    /**
     * Return the final task list representing the workflow.
     * Ensures the workflow is complete with no cycles, self-references
     *
     * @return the list
     */
    public List<Task> buildTaskList() {
        if (taskMap.values().isEmpty()) {
            throw new IllegalStateException("At least one task is required");
        }
        List<Task> tasks = tasksWithCheckpoints();

        SwiftUtil.cycleCheck(tasks);
        return tasks;
    }

    private List<Task> tasksWithCheckpoints() {
        List<Task> list = new ArrayList<>(taskMap.values());
        for (int i = 1; i <= checkpointCounter; i++) {
            Task checkpoint = new Checkpoint(i);
            for (Task task : findLeafNodes(i - 1)) {
                checkpoint.addParents(task);
            }
            for (Task task : taskMap.values()) {
                if (task.getCheckpoint() == i && task.getParents().isEmpty()) {
                    task.addParents(checkpoint);
                }
            }
            list.add(checkpoint);
        }
        return list;
    }

    private Collection<Task> findLeafNodes(int checkpoint) {
        Map<String, Task> map = new LinkedHashMap<>(taskMap);
        for (Map.Entry<String, Task> entry : taskMap.entrySet()) {
            if (entry.getValue().getCheckpoint() != checkpoint) {
                map.remove(entry.getKey());
            } else {
                for (Task parent : entry.getValue().getParents()) {
                    map.remove(parent.getId());
                }
            }
        }
        return map.values();
    }

    public static List<Task> filterTasks(Collection<Task> tasks, String regExp, boolean assertMatch) {
        List<Task> list = new ArrayList<>();
        for (Task task : tasks) {
            if (task.getId().matches(regExp)) {
                list.add(task);
            }
        }
        if (assertMatch && list.isEmpty()) {
            throw new IllegalStateException(format("Regular expression '%s' did not find any matching tasks", regExp));
        }
        return list;
    }

    private List<Task> findTasks(String regExp) {
        return filterTasks(taskMap.values(), regExp, true);
    }

    private void assertWithGroup() {
        if (withGroup.isEmpty()) {
            throw new IllegalStateException("At least one task is required before calling method");
        }
    }

    public String toString() {
        StringBuilder b = new StringBuilder(taskMap.size() + 50);
        for (Task task : tasksWithCheckpoints()) {
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
