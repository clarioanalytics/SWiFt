package com.clario.swift;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;

/**
 * Builder to create a workflow, which is a simple directed graph of {@link Task}.
 *
 * @author George Coller
 */
public class WorkflowBuilder {
    public static final Logger log = LoggerFactory.getLogger(WorkflowBuilder.class);

    private final String name;
    private final String version;
    private final Map<String, Task> taskMap = new LinkedHashMap<>();
    private final List<Task> withGroup = new ArrayList<>();
    private boolean disableCycleAssertion = false;

    /**
     * Create an instance to match an SWF-registered workflow.
     *
     * @param name registered workflow name
     * @param version registered workflow version
     */
    public WorkflowBuilder(String name, String version) {
        this.name = name;
        this.version = version;
    }

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
     * Add an {@link Activity} task, use name as id.
     * Convenience method for activities that are only used once in a workflow.
     *
     * @param name name of registered activity in SWF
     * @param version version of registered activity in SWF
     *
     * @return this instance for chaining
     */
    public WorkflowBuilder activity(String name, String version) {
        return add(new Activity(name, name, version));
    }

    /**
     * Select a set of tasks to be added as parents to the most recently added task.
     * <p/>
     * Note: Use with {@link #withTasks} to add parents to multiple children at once.
     *
     * @param regExprs one or more regular expressions to match task ids to add as parents
     *
     * @return this instance for chaining
     */
    public WorkflowBuilder addParents(String... regExprs) {
        assertWithGroup();
        for (String regExp : regExprs) {
            for (Task parent : findTasks(regExp)) {
                for (Task task : withGroup) {
                    task.addParents(parent);
                }
            }
        }
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
     * Disable the graph cycle assertion when building the workflow.
     * By default workflows detects if any routes are cycles including self-referencing tasks to avoid endless workflow executions.
     * <p/>
     * Note: it is preferable to set a {@link #retry} for tasks that should be retried on failure.
     */
    public void setDisableCycleAssertion() {
        this.disableCycleAssertion = true;
    }

    /**
     * Return the finished workflow.
     *
     * @throws AssertionError if no tasks were added to the workflow or a cycle was detected.
     * @see #setDisableCycleAssertion() to disable cycle detection
     */
    public Workflow buildWorkflow() {
        if (taskMap.values().isEmpty()) {
            throw new AssertionError("At least one task is required on workflow");
        }
        if (!disableCycleAssertion) { Vertex.assertNoCycles(taskMap.values()); }
        return new Workflow(name, version, taskMap);
    }

    /**
     * Utility function to filter a collection of tasks by applying a regular expression against each task id.
     *
     * @param tasks tasks to filter
     * @param regExp regular expression to apply
     * @param assertMatch if true, throw an exception if zero matches were found
     *
     * @return list of filtered tasks
     * @throws AssertionError if assertMatch is true and zero matches were found
     */
    public static List<Task> filterTasks(Collection<Task> tasks, String regExp, boolean assertMatch) {
        List<Task> list = new ArrayList<>();
        for (Task task : tasks) {
            if (task.getId().matches(regExp)) {
                list.add(task);
            }
        }
        if (assertMatch && list.isEmpty()) {
            throw new AssertionError(format("Regular expression '%s' did not find any matching tasks", regExp));
        }
        return list;
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
     * Set {@link Activity#setScheduleToCloseTimeout} to a task or tasks if they are an activity task.
     */
    public WorkflowBuilder scheduleCloseTimeout(TimeUnit minutes, int duration) {
        assertWithGroup();
        for (Activity activity : filterActivities(withGroup)) {
            activity.setScheduleToCloseTimeout(minutes, duration);
        }
        return this;
    }

    /**
     * Set {@link Activity#setScheduleToStartTimeout} to a task or tasks if they are an activity task.
     */
    public WorkflowBuilder scheduleToStartTimeout(TimeUnit minutes, int duration) {
        assertWithGroup();
        for (Activity activity : filterActivities(withGroup)) {
            activity.setScheduleToStartTimeout(minutes, duration);
        }
        return this;
    }

    /**
     * Set {@link Activity#setStartToCloseTimeout} to a task or tasks if they are an activity task.
     */
    public WorkflowBuilder startToCloseTimeout(TimeUnit minutes, int duration) {
        assertWithGroup();
        for (Activity activity : filterActivities(withGroup)) {
            activity.setStartToCloseTimeout(minutes, duration);
        }
        return this;
    }

    /**
     * Set {@link Activity#setHeartBeatTimeout} to a task or tasks if they are an activity task.
     */
    public WorkflowBuilder heartBeatTimeout(TimeUnit minutes, int duration) {
        assertWithGroup();
        for (Activity activity : filterActivities(withGroup)) {
            activity.setHeartBeatTimeout(minutes, duration);
        }
        return this;
    }

    private List<Activity> filterActivities(List<Task> tasks) {
        List<Activity> activities = new ArrayList<>();
        for (Task task : tasks) {
            if (task instanceof Activity) {
                activities.add((Activity) task);
            }
        }
        return activities;
    }

    private List<Task> findTasks(String regExp) {
        return filterTasks(taskMap.values(), regExp, true);
    }

    private void assertWithGroup() {
        if (withGroup.isEmpty()) {
            throw new AssertionError("At least one task is required before calling method");
        }
    }
}
