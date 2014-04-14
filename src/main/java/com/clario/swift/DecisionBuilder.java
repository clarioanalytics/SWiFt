package com.clario.swift;

import java.util.*;

import static com.clario.swift.SwiftUtil.join;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.reverse;

/**
 * @author George Coller
 */
public class DecisionBuilder {

    private static final String STEP_CLASS_NAME = DecisionStep.class.getSimpleName();
    private final Map<String, DecisionStep> stepMap = new LinkedHashMap<>();
    private final Map<String, Set<DecisionStep>> groupMap = new LinkedHashMap<>();
    private int decisionGroupCounter = 0;
    private DecisionStep last;

    public DecisionBuilder add(DecisionStep step) {
        String id = step.getStepId();
        if (stepMap.containsKey(id) || groupMap.containsKey(id)) {
            throw new IllegalArgumentException(format("Identifier already used: %s", id));
        }
        stepMap.put(id, step);
        step.setDecisionGroup(decisionGroupCounter);
        last = step;
        return this;
    }

    public DecisionBuilder activity(String stepId, String name, String version) {
        DecisionStep step = new ActivityDecisionStep(stepId, name, version);
        return add(step);
    }

    public DecisionBuilder activity(String stepId, String name) {
        List<DecisionStep> values = new ArrayList<>(stepMap.values());
        reverse(values);
        for (DecisionStep value : values) {
            if (value instanceof ActivityDecisionStep) {
                ActivityDecisionStep activity = (ActivityDecisionStep) value;
                if (name.equals(activity.getName())) {
                    return add(new ActivityDecisionStep(stepId, name, activity.getVersion()));
                }
            }
        }
        throw new IllegalArgumentException(format("Activity '%s' not found, version required for %s '%s'", name, STEP_CLASS_NAME, stepId));
    }

    public DecisionBuilder retry(int times, long retryWaitInMillis) {
        assertLastExists();
        last.retryTimes = times;
        last.retryWaitInMillis = retryWaitInMillis;
        return this;
    }

    public DecisionBuilder parent(String... ids) {
        assertLastExists();
        for (String id : ids) {
            for (DecisionStep parent : stepsFor(id)) {
                last.addParents(parent);
            }
        }
        return this;
    }

    public DecisionBuilder groupAndParent(String group, String parent) {
        group(group);
        parent(parent);
        return this;
    }

    DecisionBuilder group(String groupId) {
        assertLastExists();
        if (stepMap.containsKey(groupId)) {
            throw new IllegalArgumentException(format("Identifier must be unique or point to an existing group: " + groupId));
        }
        if (!groupMap.containsKey(groupId)) {
            groupMap.put(groupId, new TreeSet<DecisionStep>());
        }
        groupMap.get(groupId).add(last);
        return this;
    }

    public DecisionBuilder mark() {
        decisionGroupCounter++;
        return this;
    }

    public String toString() {
        StringBuilder b = new StringBuilder(stepMap.size() + 50);
        int decisionGroup = 0;
        for (DecisionStep step : stepMap.values()) {
            if (step.getDecisionGroup() > decisionGroup) {
                decisionGroup = step.getDecisionGroup();
                b.append(format("Decision Group %s\n", decisionGroup));
            }
            b.append(step.getStepId());
            if (step instanceof ActivityDecisionStep) {
                ActivityDecisionStep activity = (ActivityDecisionStep) step;
                b.append(format(" '%s' '%s'", activity.getName(), activity.getVersion()));
            }
            if (!step.getChildren().isEmpty()) {
                List<String> kids = new ArrayList<>();
                for (DecisionStep child : step.getChildren()) {
                    kids.add(child.getStepId());
                }
                b.append(format(" children(%s)", join(kids, ", ")));
            }
            if (!step.getParents().isEmpty()) {
                List<String> parents = new ArrayList<>();
                for (DecisionStep child : step.getParents()) {
                    parents.add(child.getStepId());
                }
                b.append(format(" parents(%s)", join(parents, ", ")));
            }
            b.append('\n');
        }
        return b.toString();
    }

    private Collection<DecisionStep> stepsFor(String id) {
        if (groupMap.containsKey(id)) {
            return groupMap.get(id);
        } else if (stepMap.containsKey(id)) {
            return asList(stepMap.get(id));
        } else {
            throw new IllegalArgumentException(STEP_CLASS_NAME + " or group id does not exist: " + id);
        }
    }

    private void assertLastExists() {
        if (last == null) {
            throw new IllegalStateException(format("%s required before calling method", STEP_CLASS_NAME));
        }
    }
}
