package com.clario.swift.examples;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.clario.swift.Workflow;
import com.clario.swift.action.SwfAction;
import com.clario.swift.action.SwfActivity;

import java.util.Arrays;
import java.util.List;

import static com.clario.swift.SwiftUtil.createCompleteWorkflowExecutionDecision;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.HOURS;

/**
 * @author George Coller
 */
public class DemoWorkflow extends Workflow {

    public static void main(String[] args) {
        Workflow workflow = new DemoWorkflow()
            .withDomain("dev-clario")
            .withTaskList("default");
        Config.register(workflow);
        Config.submit(workflow, "100");
    }

    private final SwfActivity step1 = new SwfActivity("step1", "Activity X", "1.0").withStartToCloseTimeout(HOURS, 2);
    private final SwfActivity step2a = new SwfActivity("step2a", "Activity Y", "1.0").withStartToCloseTimeout(HOURS, 2);
    private final SwfActivity step2b = new SwfActivity("step2b", "Activity Y", "1.0").withStartToCloseTimeout(HOURS, 2);
    private final SwfActivity step3 = new SwfActivity("step3", "Activity Z", "1.0").withStartToCloseTimeout(HOURS, 2);
    private final SwfActivity step4 = new SwfActivity("step4", "Activity X", "1.0").withStartToCloseTimeout(HOURS, 2);
    private final SwfActivity step5 = new SwfActivity("step5", "Activity Y", "1.0").withStartToCloseTimeout(HOURS, 2);

    public DemoWorkflow() {
        super("Demo Workflow", "1.0");
    }

    public List<SwfAction> getActions() {
        return asList((SwfAction) step1, step2a, step2b, step3, step4, step5);
    }

    public List<String> getPollingCheckpoints() {
        return Arrays.asList("step3");
    }

    @Override
    public void decide(List<Decision> decisions) {
        // jump ahead on step 3 finish
        if (!step3.isFinished()) {
            if (step1.withInput(getWorkflowInput()).decide(decisions)) {
                String step1Output = step1.getOutput();
                // Example Split
                if (step2a.withInput(step1Output).decide(decisions)
                    && step2b.withInput(step1Output).decide(decisions)) {
                    long step2aOutput = Long.parseLong(step2a.getOutput());
                    long step2bOutput = Long.parseLong(step2b.getOutput());
                    // make a decision on which output to use
                    String input = String.valueOf(Math.max(step2aOutput, step2bOutput));
                    step3.withInput(input).decide(decisions);
                }
            }
        } else {
            String step3Output = step3.getOutput();

            // Example choice
            String result;
            if ("apple".equals(step3Output)) {
                if (!step4.withInput(step3Output).decide(decisions)) {
                    return;
                }
                result = step4.getOutput();
            } else {
                if (!step5.withInput(step3Output).decide(decisions)) {
                    return;
                }
                result = step5.getOutput();
            }
            decisions.add(createCompleteWorkflowExecutionDecision(result));
        }
    }
}
