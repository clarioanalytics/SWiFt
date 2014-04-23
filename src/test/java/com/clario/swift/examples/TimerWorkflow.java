package com.clario.swift.examples;

import com.amazonaws.services.simpleworkflow.model.ChildPolicy;
import com.amazonaws.services.simpleworkflow.model.Decision;
import com.clario.swift.Workflow;
import com.clario.swift.action.ActivityAction;
import com.clario.swift.action.TimerAction;

import java.util.List;
import java.util.concurrent.TimeUnit;

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
//        Config.register(workflow);
        Config.submit(workflow, "100");
    }

    private final ActivityAction step1 = new ActivityAction("step1", "Activity X", "1.0").withStartToCloseTimeout(MINUTES, 2);
    private final ActivityAction step2 = new ActivityAction("step2", "Activity Y", "1.0").withStartToCloseTimeout(MINUTES, 2);
    private final TimerAction timerStep1 = new TimerAction("timer1").withStartToFireTimeout(SECONDS, 30);


    public TimerWorkflow() {
        super("Timer Workflow", "1.0");
        addActions(step1, step2, timerStep1);
    }

    @Override
    public void decide(List<Decision> decisions) {
        String input = getWorkflowInput();
        String output;
        if (step1.withInput(input).decide(decisions).isSuccess()) {
            output = step1.getOutput();

            // Timer makes workflow sleep for 30 seconds
            if (timerStep1.decide(decisions).isSuccess()) {
                if (step2.withInput(output).decide(decisions).isSuccess()) {
                    output = step2.getOutput();
                    decisions.add(createCompleteWorkflowExecutionDecision(output));
                }
            }
        }
    }
}
