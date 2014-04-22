package com.clario.swift.examples;

import com.amazonaws.services.simpleworkflow.model.ChildPolicy;
import com.amazonaws.services.simpleworkflow.model.Decision;
import com.clario.swift.SwiftUtil;
import com.clario.swift.Workflow;
import com.clario.swift.action.SwfAction;
import com.clario.swift.action.SwfActivity;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * @author George Coller
 */
public class SimpleWorkflow extends Workflow {

    public static void main(String[] args) {
        Workflow workflow = new SimpleWorkflow()
            .withDomain("dev-clario")
            .withTaskList("default")
            .withExecutionStartToCloseTimeout(TimeUnit.MINUTES, 30)
            .withTaskStartToCloseTimeout(TimeUnit.MINUTES, 30)
            .withChildPolicy(ChildPolicy.TERMINATE);
        Config.register(workflow);
        Config.submit(workflow, "100");
    }

    private final SwfActivity step1 = new SwfActivity("step1", "Activity X", "1.0").withStartToCloseTimeout(MINUTES, 2);
    private final SwfActivity step2 = new SwfActivity("step2", "Activity Y", "1.0").withStartToCloseTimeout(MINUTES, 2);
    private final SwfActivity step3 = new SwfActivity("step3", "Activity Z", "1.0").withStartToCloseTimeout(MINUTES, 2);


    public SimpleWorkflow() {
        super("Simple Workflow", "1.0");
    }

    @Override
    public List<SwfAction> getActions() {
        return asList((SwfAction) step1, step2, step3);
    }

    @Override
    public void decide(List<Decision> decisions) {
        String input = getWorkflowInput();
        if (step1.withInput(input).decide(decisions)) {
            if (step2.withInput(step1.getOutput()).decide(decisions)) {
                if (step3.withInput(step2.getOutput()).decide(decisions)) {
                    decisions.add(SwiftUtil.createCompleteWorkflowExecutionDecision(step3.getOutput()));
                }
            }
        }
    }
}
