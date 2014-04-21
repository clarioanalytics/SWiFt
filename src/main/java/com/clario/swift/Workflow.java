package com.clario.swift;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.amazonaws.services.simpleworkflow.model.HistoryEvent;

import java.util.Arrays;
import java.util.List;

import static com.clario.swift.SwiftUtil.createCompleteWorkflowExecutionDecision;
import static com.clario.swift.SwiftUtil.makeKey;
import static java.util.concurrent.TimeUnit.HOURS;

/**
 * @author George Coller
 */
public class Workflow {
    private final SwiftActivity step1 = new SwiftActivity("step1", "Activity X", "1.0").withStartToCloseTimeout(HOURS, 2);
    private final SwiftActivity step2a = new SwiftActivity("step2a", "Activity Y", "1.0").withStartToCloseTimeout(HOURS, 2);
    private final SwiftActivity step2b = new SwiftActivity("step2b", "Activity Y", "1.0").withStartToCloseTimeout(HOURS, 2);
    private final SwiftActivity step3 = new SwiftActivity("step3", "Activity Z", "1.0").withStartToCloseTimeout(HOURS, 2);
    private final SwiftActivity step4 = new SwiftActivity("step4", "Activity X", "1.0").withStartToCloseTimeout(HOURS, 2);
    private final SwiftActivity step5 = new SwiftActivity("step5", "Activity Y", "1.0").withStartToCloseTimeout(HOURS, 2);

    private final String name;
    private final String version;
    private final String taskList;
    private HistoryInspector historyInspector = new HistoryInspector();

    public Workflow(String name, String version, String taskList) {
        this.name = name;
        this.version = version;
        this.taskList = taskList;
    }

    public List<SwiftActivity> getActivities() {
        return Arrays.asList(step1, step2a, step2b, step3, step4, step5);
    }

    public String getWorkflowKey() {
        return makeKey(name, version);
    }


    public void decide(String workflowId, List<Decision> decisions) {
        // jump ahead on step 3 finish
        if (!step3.isFinished()) {
            if (step1.decided(decisions, historyInspector.getWorkflowInput())) {
                String step1Output = step1.getOutput();
                // Example Split
                if (step2a.decided(decisions, step1Output) && step2b.decided(decisions, step1Output)) {
                    long step2aOutput = Long.parseLong(step2a.getOutput());
                    long step2bOutput = Long.parseLong(step2b.getOutput());
                    // make a decision on which output to use
                    String input = String.valueOf(Math.max(step2aOutput, step2bOutput));
                    step3.decided(decisions, input);
                }
            }
        } else {
            String step3Output = step3.getOutput();

            // Example choice
            String result;
            if ("apple".equals(step3Output)) {
                if (!step4.decided(decisions, step3Output)) {
                    return;
                }
                result = step4.getOutput();
            } else {
                if (!step5.decided(decisions, step3Output)) {
                    return;
                }
                result = step5.getOutput();
            }
            decisions.add(createCompleteWorkflowExecutionDecision(result));
        }
    }

    public String getTaskList() {
        return taskList;
    }

    /**
     * List of activity ids or markers used to tell poller to stop polling for more history.
     */
    public List<String> getPollingCheckpoints() {
        return Arrays.asList("step3");
    }

    public void reset() {
        for (SwiftActivity activity : getActivities()) {
            activity.setWorkflow(this);
        }
        historyInspector.reset();
    }

    public HistoryInspector getHistoryInspector() { return historyInspector; }

    public boolean isMoreHistoryRequired() {
        for (String checkPoint : getPollingCheckpoints()) {
            if (!historyInspector.taskEvents(checkPoint).isEmpty() || historyInspector.getMarkers().containsKey(checkPoint)) {
                return false;
            }
        }
        return true;
    }

    public void addHistoryEvents(List<HistoryEvent> events) {
        historyInspector.addHistoryEvents(events);
    }

    public List<String> getSchedulingErrors() {
        return historyInspector.getScheduleActivityErrors();
    }
}
