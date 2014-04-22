package com.clario.swift.examples;

import com.amazonaws.services.simpleworkflow.model.ChildPolicy;
import com.amazonaws.services.simpleworkflow.model.Decision;
import com.clario.swift.SwiftUtil;
import com.clario.swift.Workflow;
import com.clario.swift.action.SwfAction;
import com.clario.swift.action.SwfActivity;
import com.clario.swift.action.SwfMarker;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * @author George Coller
 */
public class MarkerWorkflow extends Workflow {

    public static void main(String[] args) {
        Workflow workflow = new MarkerWorkflow()
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
    private final SwfMarker marker1 = new SwfMarker("marker1").withDetails("123");
    private final SwfMarker marker2 = new SwfMarker("marker2").withDetails("456");

    public MarkerWorkflow() {
        super("Timer Workflow", "1.0");
    }

    @Override
    public List<SwfAction> getActions() {
        return asList(step1, step2, marker1, marker2);
    }

    @Override
    public void decide(List<Decision> decisions) {
        String input = getWorkflowInput();
        String output;
        if (step1.withInput(input).decide(decisions)) {
            output = step1.getOutput();

            if (marker1.decide(decisions) && marker2.decide(decisions)) {
                if (step2.withInput(output).decide(decisions)) {
                    output = step2.getOutput();
                    decisions.add(SwiftUtil.createCompleteWorkflowExecutionDecision(output));
                }
            }
        }
    }
}
