package com.clario.swift.examples;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.clario.swift.Workflow;
import com.clario.swift.action.ActivityAction;

import java.util.List;

import static com.clario.swift.examples.Config.*;
import static java.util.concurrent.TimeUnit.HOURS;
import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * @author George Coller
 */
public class PollingCheckpointWorkflow extends Workflow {

    public static void main(String[] args) {
        Workflow workflow = new PollingCheckpointWorkflow()
            .withDomain(SWIFT_DOMAIN)
            .withTaskList(SWIFT_TASK_LIST)
            .withExecutionStartToCloseTimeout(MINUTES, 5)
            .withTaskStartToCloseTimeout(MINUTES, 1);
        submit(workflow, "100");
    }

    private final ActivityAction step1 = new ActivityAction("step1", "Activity X", "1.0").withStartToCloseTimeout(HOURS, 2);
    private final ActivityAction step2a = new ActivityAction("step2a", "Activity Y", "1.0").withStartToCloseTimeout(HOURS, 2);
    private final ActivityAction step2b = new ActivityAction("step2b", "Activity Y", "1.0").withStartToCloseTimeout(HOURS, 2);
    private final ActivityAction step3 = new ActivityAction("step3", "Activity Z", "1.0").withStartToCloseTimeout(HOURS, 2);
    private final ActivityAction step4 = new ActivityAction("step4", "Activity X", "1.0").withStartToCloseTimeout(HOURS, 2);
    private final ActivityAction step5 = new ActivityAction("step5", "Activity Y", "1.0").withStartToCloseTimeout(HOURS, 2);

    public PollingCheckpointWorkflow() {
        super("Polling Checkpoint Workflow", "1.0");
        addActions(step1, step2a, step2b, step3, step4, step5);
    }

    @Override
    public boolean isContinuePollingForHistoryEvents() {
        return !step3.isSuccess();
    }

    @Override
    public void decide(List<Decision> decisions) {
        // jump ahead on step 3 finish
        if (!step3.isSuccess()) {
            if (step1.withInput(getWorkflowInput()).decide(decisions).isSuccess()) {
                String step1Output = step1.getOutput();
                // Example Split
                if (step2a.withInput(step1Output).decide(decisions).isSuccess()
                    && step2b.withInput(step1Output).decide(decisions).isSuccess()) {
                    long step2aOutput = Long.parseLong(step2a.getOutput());
                    long step2bOutput = Long.parseLong(step2b.getOutput());
                    // make a decision on which output to use and join to step3
                    String input = String.valueOf(Math.max(step2aOutput, step2bOutput));
                    step3.withInput(input).decide(decisions);
                }
            }
        } else {
            String step3Output = step3.getOutput();

            // Example choice
            String result;
            if ("apple".equals(step3Output)) {
                if (!step4.withInput(step3Output).decide(decisions).isSuccess()) {
                    return;
                }
                result = step4.getOutput();
            } else {
                if (!step5.withInput(step3Output).decide(decisions).isSuccess()) {
                    return;
                }
                result = step5.getOutput();
            }
            decisions.add(createCompleteWorkflowExecutionDecision(result));
        }
    }
}
