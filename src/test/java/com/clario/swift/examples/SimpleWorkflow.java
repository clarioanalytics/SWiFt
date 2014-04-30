package com.clario.swift.examples;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.clario.swift.Workflow;
import com.clario.swift.action.ActivityAction;

import java.util.List;

import static com.amazonaws.services.simpleworkflow.model.ChildPolicy.TERMINATE;
import static com.clario.swift.examples.Config.*;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author George Coller
 */
public class SimpleWorkflow extends Workflow {

    public static void main(String[] args) {
        Workflow workflow = new SimpleWorkflow()
            .withDomain(SWIFT_DOMAIN)
            .withTaskList(SWIFT_TASK_LIST)
            .withExecutionStartToCloseTimeout(MINUTES, 1)
            .withTaskStartToCloseTimeout(SECONDS, 20)
            .withChildPolicy(TERMINATE);
        submit(workflow, "100");
    }

    private final ActivityAction step1 = new ActivityAction("step1", "Activity X", "1.0");
    private final ActivityAction step2 = new ActivityAction("step2", "Activity Y", "1.0");
    private final ActivityAction step3 = new ActivityAction("step3", "Activity Z", "1.0").withCompleteWorkflowOnSuccess();


    public SimpleWorkflow() {
        super("Simple Workflow", "1.0");
        addActions(step1, step2, step3);
    }

    @Override
    public void decide(List<Decision> decisions) {
        String input = getWorkflowInput();
        if (step1.withInput(input).decide(decisions).isSuccess()) {
            if (step2.withInput(step1.getOutput()).decide(decisions).isSuccess()) {
                // step3 will add a complete or fail workflow decision depending on its final state
                step3.withInput(step2.getOutput()).decide(decisions);
            }
        }
    }
}
