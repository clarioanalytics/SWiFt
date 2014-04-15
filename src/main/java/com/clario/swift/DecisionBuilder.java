package com.clario.swift;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.clario.swift.SwiftUtil.join;
import static java.lang.String.format;
import static java.util.Collections.reverse;

/**
 * @author George Coller
 */
public class DecisionBuilder {
    public static final Logger log = LoggerFactory.getLogger(DecisionBuilder.class);

    private static final String STEP_CLASS_NAME = DecisionStep.class.getSimpleName();
    private final Map<String, DecisionStep> stepMap = new LinkedHashMap<>();
    private final List<DecisionStep> withGroup = new ArrayList<>();
    private int decisionGroupCounter = 0;

    public DecisionBuilder add(DecisionStep step) {
        String id = step.getStepId();
        if (stepMap.containsKey(id)) {
            throw new IllegalArgumentException(format("%s '%s' already added", STEP_CLASS_NAME, id));
        }
        stepMap.put(id, step);
        step.setDecisionGroup(decisionGroupCounter);
        withGroup.clear();
        withGroup.add(step);
        return this;
    }

    public DecisionBuilder activity(String stepId, String name, String version) {
        return add(new ActivityDecisionStep(stepId, name, version));
    }

    public DecisionBuilder activity(String stepId, String name) {
        // look up most recent activity with matching name and use its version
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
        assertWithGroup();
        for (DecisionStep step : withGroup) {
            step.addRetry(times, retryWaitInMillis);
        }
        return this;
    }

    public DecisionBuilder addParents(String parentRegExp) {
        assertWithGroup();
        List<DecisionStep> byMatchStepId = findSteps(parentRegExp);
        for (DecisionStep parent : byMatchStepId) {
            for (DecisionStep step : withGroup) {
                step.addParents(parent);
            }
        }
        return this;
    }

    public DecisionBuilder mark() {
        decisionGroupCounter++;
        return this;
    }

    // TODO: Put asserts in here to check cycles, self references, or zero size, add in DecisionGroup steps
    public ArrayList<DecisionStep> buildStepList() {
        ArrayList<DecisionStep> decisionSteps = new ArrayList<>(stepMap.values());
        SwiftUtil.cycleCheck(decisionSteps);
        return decisionSteps;
    }

    public DecisionBuilder withEach(String regExp) {
        withGroup.clear();
        withGroup.addAll(findSteps(regExp));
        assertWithGroup();
        return this;
    }

    public DecisionBuilder withEach(String... ids) {
        withGroup.clear();
        for (String id : ids) {
            if (stepMap.containsKey(id)) {
                withGroup.add(stepMap.get(id));
            } else {
                throw new IllegalArgumentException(format("%s '%s' not found", STEP_CLASS_NAME, id));
            }
        }
        assertWithGroup();
        return this;
    }

    public static List<DecisionStep> filterSteps(Collection<DecisionStep> steps, String regExp, boolean assertMatch) {
        List<DecisionStep> list = new ArrayList<>();
        for (DecisionStep step : steps) {
            if (step.getStepId().matches(regExp)) {
                list.add(step);
            }
        }
        if (assertMatch && list.isEmpty()) {
            throw new IllegalStateException(format("Regular expression '%s' did not find any matching %ss", regExp, STEP_CLASS_NAME));
        }
        return list;
    }

    private List<DecisionStep> findSteps(String regExp) {
        return filterSteps(stepMap.values(), regExp, true);
    }

    private void assertWithGroup() {
        if (withGroup.isEmpty()) {
            throw new IllegalStateException(format("%s required before calling method", STEP_CLASS_NAME));
        }
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
//            if (!step.getChildren().isEmpty()) {
//                List<String> kids = new ArrayList<>();
//                for (DecisionStep child : step.getChildren()) {
//                    kids.add(child.getStepId());
//                }
//                b.append(format(" children(%s)", join(kids, ", ")));
//            }
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
}
