package com.clario.swift.examples.workflows;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.clario.swift.DecisionPoller;
import com.clario.swift.Workflow;
import com.clario.swift.action.Action;
import com.clario.swift.action.ActivityAction;

import java.util.List;

import static com.amazonaws.services.simpleworkflow.model.ChildPolicy.TERMINATE;
import static com.clario.swift.examples.Config.config;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * SWiFt "Hello World" example workflow that does three activities one after the other and then completes.
 * <p/>
 * The important concept to learn here is that the {@link @Workflow#decide} method gets called by a {@link DecisionPoller}
 * every time SWF creates a <code>DecisionTaskScheduled</code> event.
 * <p/>
 * Before each call to {@code decide} the workflow will call {@link Action#setWorkflow} with itself as the parameter,
 * which gives the action access to the workflow history, its current state, etc.
 * <p/>
 * Alternatively, you could manually call {@link Action#setWorkflow} inside the <code>decide</code> method.
 * This is useful if your workflow creates actions on the fly.  {@link SignalWaitForSignalWorkflow} has such an example.
 *
 * @author George Coller
 */
public class SimpleWorkflow extends Workflow {

    public static void main(String[] args) {
        Workflow workflow = new SimpleWorkflow()
            .withDomain(config.getDomain())
            .withTaskList(config.getTaskList())
            .withExecutionStartToCloseTimeout(MINUTES, 5)
            .withTaskStartToCloseTimeout(SECONDS, 30)
            .withChildPolicy(TERMINATE)
            .withDescription("A Simple Example Workflow");
        config.submit(workflow, "100");
    }

    // Create known actions as fields
    final ActivityAction step1 = new ActivityAction("step1", "Activity X", "1.0");
    final ActivityAction step2 = new ActivityAction("step2", "Activity Y", "1.0");
    final ActivityAction step3 = new ActivityAction("step3", "Activity Z", "1.0");


    /** Start the workflow by submitting it to SWF. */
    public SimpleWorkflow() {
        super("Simple Workflow", "1.0");

        // This step registers the steps with the workflow so that you don't manually have to
        // inject their workflow, history, state with each call to decide()
        addActions(step1, step2, step3);
    }

    @Override
    public void decide(List<Decision> decisions) {
        String input = getWorkflowInput();

        // Do step1 with workflow input, continue on when "Activity X" is complete
        if (step1.withInput(input)
            .decide(decisions)
            .isSuccess()) {

            // Do step2 with step1's output, continue on when "Activity Y" is complete
            if (step2.withInput(step1.getOutput())
                .decide(decisions)
                .isSuccess()) {

                // Do step3 with step2's output, complete the workflow when "Activity Z" is complete
                step3.withInput(step2.getOutput())
                    .withCompleteWorkflowOnSuccess() // this tells the action to create the complete workflow decision
                    .decide(decisions);
            }
        }

        // decide is called multiple times as a workflow progresses
        // you can put a breakpoint or log statement here to see what has been
        // decided by inspecting the decisions list.
    }
}
