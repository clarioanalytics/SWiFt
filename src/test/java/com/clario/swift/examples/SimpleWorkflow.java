package com.clario.swift.examples;

import com.amazonaws.services.simpleworkflow.model.ChildPolicy;
import com.amazonaws.services.simpleworkflow.model.Decision;
import com.clario.swift.Workflow;
import com.clario.swift.action.ActivityAction;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author George Coller
 */
public class SimpleWorkflow extends Workflow {

    public static void main(String[] args) {
        Workflow workflow = new SimpleWorkflow()
            .withDomain("dev-clario")
            .withTaskList("default")
            .withExecutionStartToCloseTimeout(TimeUnit.MINUTES, 1)
            .withTaskStartToCloseTimeout(TimeUnit.SECONDS, 20)
            .withChildPolicy(ChildPolicy.TERMINATE);
        Config.submit(workflow, "100");
    }

    private final ActivityAction step1 = new ActivityAction("step1", "Activity X", "1.0");
    private final ActivityAction step2 = new ActivityAction("step2", "Activity Y", "1.0");
    private final ActivityAction step3 = new ActivityAction("step3", "Activity Z", "1.0");


    public SimpleWorkflow() {
        super("Simple Workflow", "2.0");
        addActions(step1, step2, step3);
    }

    @Override
    public void decide(List<Decision> decisions) {
        String input = getWorkflowInput();
        if (step1.withInput(input).decide(decisions).isSuccess()) {
            if (step2.withInput(step1.getOutput()).decide(decisions).isSuccess()) {
                if (step3.withInput(step2.getOutput()).decide(decisions).isSuccess()) {
                    decisions.add(createCompleteWorkflowExecutionDecision(step3.getOutput()));
                }
            }
        }
    }
}
