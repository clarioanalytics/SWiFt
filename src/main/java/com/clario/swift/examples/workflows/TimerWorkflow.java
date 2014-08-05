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

    /** Start the workflow by submitting it to SWF. */
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

    final ActivityAction beforeTimer = new ActivityAction("step1", "Activity X", "1.0").withStartToCloseTimeout(MINUTES, 2);
    final ActivityAction afterTimer = new ActivityAction("step2", "Activity Y", "1.0").withStartToCloseTimeout(MINUTES, 2);
    final TimerAction timerAction = new TimerAction("timer1")
        .withStartToFireTimeout(SECONDS, 30)
        .withControl("Sample Timer Control Value");


    public TimerWorkflow() {
        super("Timer Workflow", "1.0");
        addActions(beforeTimer, afterTimer, timerAction);
    }

    @Override
    public void decide(List<Decision> decisions) {
        String input = getWorkflowInput();
        String output;
        if (beforeTimer.withInput(input).decide(decisions).isSuccess()) {
            output = beforeTimer.getOutput();

            // Timer makes workflow sleep for 30 seconds
            if (timerAction.decide(decisions).isSuccess()) {

                // 30 seconds have passed now decide step2 and then finish
                afterTimer.withInput(output)
                    .withCompleteWorkflowOnSuccess()
                    .decide(decisions);
            }
        }
    }
}
