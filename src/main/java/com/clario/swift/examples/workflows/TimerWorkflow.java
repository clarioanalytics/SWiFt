package com.clario.swift.examples.workflows;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.clario.swift.Workflow;
import com.clario.swift.action.ActivityAction;
import com.clario.swift.action.TimerAction;

import java.util.List;

import static com.amazonaws.services.simpleworkflow.model.ChildPolicy.TERMINATE;
import static com.clario.swift.examples.Config.config;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Example of using a {@link TimerAction} in a workflow.
 *
 * @author George Coller
 */
public class TimerWorkflow extends Workflow {

    public static void main(String[] args) {
        Workflow workflow = new TimerWorkflow()
            .withDomain(config.getDomain())
            .withDomain(config.getDomain())
            .withTaskList(config.getTaskList())
            .withExecutionStartToCloseTimeout(MINUTES, 30)
            .withTaskStartToCloseTimeout(MINUTES, 30)
            .withChildPolicy(TERMINATE);
        config.submit(workflow, "100");
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

                // 30 seconds have passed now decide step2 and then finish
                step2.withInput(output)
                    .withCompleteWorkflowOnSuccess()
                    .decide(decisions);
            }
        }
    }
}
