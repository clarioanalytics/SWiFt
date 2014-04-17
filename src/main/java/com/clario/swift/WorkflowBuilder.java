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
     * Select a set of tasks as parents.
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
     * Return the finished workflow.
     */
    public Workflow buildWorkflow() {
        if (taskMap.values().isEmpty()) {
            throw new IllegalStateException("At least one task is required on workflow");
        }
        SwiftUtil.cycleCheck(taskMap.values());
        return new Workflow(name, version, taskMap);
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


    public WorkflowBuilder scheduleCloseTimeout(TimeUnit minutes, int i) {
        assertWithGroup();
        for (Activity activity : filterActivities(withGroup)) {
            activity.setScheduleToCloseTimeout(minutes, i);
        }
        return this;
    }

    public WorkflowBuilder scheduleToStartTimeout(TimeUnit minutes, int i) {
        assertWithGroup();
        for (Activity activity : filterActivities(withGroup)) {
            activity.setScheduleToStartTimeout(minutes, i);
        }
        return this;
    }

    public WorkflowBuilder startToCloseTimeout(TimeUnit minutes, int i) {
        assertWithGroup();
        for (Activity activity : filterActivities(withGroup)) {
            activity.setStartToCloseTimeout(minutes, i);
        }
        return this;
    }

    public WorkflowBuilder heartBeatTimeout(TimeUnit minutes, int i) {
        assertWithGroup();
        for (Activity activity : filterActivities(withGroup)) {
            activity.setHeartBeatTimeout(minutes, i);
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
            throw new IllegalStateException("At least one task is required before calling method");
        }
    }
}
