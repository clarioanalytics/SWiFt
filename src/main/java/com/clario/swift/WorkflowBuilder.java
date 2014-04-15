package com.clario.swift;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.clario.swift.SwiftUtil.join;
import static java.lang.String.format;
import static java.util.Collections.reverse;

/**
 * Builder to create a graph of {@link Task}.
 *
 * @author George Coller
 */
public class WorkflowBuilder {
    public static final Logger log = LoggerFactory.getLogger(WorkflowBuilder.class);

    private static final String TASK_CLASS_NAME = Task.class.getSimpleName();
    private final Map<String, Task> taskMap = new LinkedHashMap<>();
    private final List<Task> withGroup = new ArrayList<>();
    private int breakpointCounter = 0;

    public WorkflowBuilder add(Task task) {
        String id = task.getId();
        if (taskMap.containsKey(id)) {
            throw new IllegalArgumentException(format("%s '%s' already added", TASK_CLASS_NAME, id));
        }
        taskMap.put(id, task);
        task.setBreakpoint(breakpointCounter);
        withGroup.clear();
        withGroup.add(task);
        return this;
    }

    public WorkflowBuilder activity(String id, String name, String version) {
        return add(new Activity(id, name, version));
    }

    public WorkflowBuilder activity(String id, String name) {
        // look up most recent activity with matching name and use its version
        List<Task> values = new ArrayList<>(taskMap.values());
        reverse(values);
        for (Task value : values) {
            if (value instanceof Activity) {
                Activity activity = (Activity) value;
                if (name.equals(activity.getName())) {
                    return add(new Activity(id, name, activity.getVersion()));
                }
            }
        }
        throw new IllegalArgumentException(format("Activity '%s' not found, version required for %s '%s'", name, TASK_CLASS_NAME, id));
    }

    public WorkflowBuilder retry(int times, long retryWaitInMillis) {
        assertWithGroup();
        for (Task task : withGroup) {
            task.addRetry(times, retryWaitInMillis);
        }
        return this;
    }

    public WorkflowBuilder addParents(String parentRegExp) {
        assertWithGroup();
        List<Task> taskList = findTasks(parentRegExp);
        for (Task parent : taskList) {
            for (Task task : withGroup) {
                task.addParents(parent);
            }
        }
        return this;
    }

    public WorkflowBuilder mark() {
        breakpointCounter++;
        return this;
    }

    // TODO: Put asserts in here to check cycles, self references, or zero size, add in breakpoint tasks
    public ArrayList<Task> buildTaskList() {
        ArrayList<Task> tasks = new ArrayList<>(taskMap.values());
        SwiftUtil.cycleCheck(tasks);
        return tasks;
    }

    public WorkflowBuilder withEach(String... regExprs) {
        withGroup.clear();
        for (String regExp : regExprs) {
            withGroup.addAll(findTasks(regExp));
        }
        assertWithGroup();
        return this;
    }

    public static List<Task> filterTasks(Collection<Task> tasks, String regExp, boolean assertMatch) {
        List<Task> list = new ArrayList<>();
        for (Task task : tasks) {
            if (task.getId().matches(regExp)) {
                list.add(task);
            }
        }
        if (assertMatch && list.isEmpty()) {
            throw new IllegalStateException(format("Regular expression '%s' did not find any matching %ss", regExp, TASK_CLASS_NAME));
        }
        return list;
    }

    private List<Task> findTasks(String regExp) {
        return filterTasks(taskMap.values(), regExp, true);
    }

    private void assertWithGroup() {
        if (withGroup.isEmpty()) {
            throw new IllegalStateException(format("%s required before calling method", TASK_CLASS_NAME));
        }
    }

    public String toString() {
        StringBuilder b = new StringBuilder(taskMap.size() + 50);
        int breakpoint = 0;
        for (Task task : taskMap.values()) {
            if (task.getBreakpoint() > breakpoint) {
                breakpoint = task.getBreakpoint();
                b.append(format("Breakpoint%s\n", breakpoint));
            }
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
