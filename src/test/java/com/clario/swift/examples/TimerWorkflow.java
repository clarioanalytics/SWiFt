package com.clario.swift.examples;

import com.amazonaws.services.simpleworkflow.model.ChildPolicy;
import com.amazonaws.services.simpleworkflow.model.Decision;
import com.clario.swift.SwiftUtil;
import com.clario.swift.Workflow;
import com.clario.swift.action.SwfAction;
import com.clario.swift.action.SwfActivity;
import com.clario.swift.action.SwfTimer;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * @author George Coller
 */
public class TimerWorkflow extends Workflow {

    public static void main(String[] args) {
        Workflow workflow = new TimerWorkflow()
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
    private final SwfTimer timerStep1 = new SwfTimer("timer1").withStartToFireTimeout(SECONDS, 30);


    public TimerWorkflow() {
        super("Timer Workflow", "1.0");
    }

    @Override
    public List<SwfAction> getActions() {
        return asList(step1, step2, timerStep1);
    }

    @Override
    public void decide(List<Decision> decisions) {
        String input = getWorkflowInput();
        String output;
        if (step1.withInput(input).decide(decisions)) {
            output = step1.getOutput();

            // Timer makes workflow sleep for 30 seconds
            if (timerStep1.decide(decisions)) {
                if (step2.withInput(output).decide(decisions)) {
                    output = step2.getOutput();
                    decisions.add(SwiftUtil.createCompleteWorkflowExecutionDecision(output));
                }
            }
        }
    }
}
