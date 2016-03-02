package com.clario.swift.examples.workflows;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.clario.swift.DecisionBuilder;
import com.clario.swift.Workflow;
import com.clario.swift.action.ActionFn;
import com.clario.swift.action.ActivityAction;

import java.util.List;

import static com.amazonaws.services.simpleworkflow.model.ChildPolicy.TERMINATE;
import static com.clario.swift.examples.Config.config;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Example demonstrating {@link Workflow#sequence(List, ActionFn[])} method, which can simplify repetitive workflow code.
 *
 * @author George Coller
 * @see SimpleWorkflow for the long version of this workflow
 */
public class SimpleWorkflowSequenced extends Workflow {

    public static void main(String[] args) {
        Workflow workflow = new SimpleWorkflowSequenced()
                                .withDomain(config().getDomain())
                                .withTaskList(config().getTaskList())
                                .withExecutionStartToCloseTimeout(MINUTES, 5)
                                .withTaskStartToCloseTimeout(SECONDS, 30)
                                .withChildPolicy(TERMINATE)
                                .withDescription("A Simple Example Workflow");
        config().submit(workflow, "100");
    }

    // Create known actions as fields
    final ActivityAction step1 = new ActivityAction("step1", "Activity X", "1.0");
    final ActivityAction step2 = new ActivityAction("step2", "Activity Y", "1.0");
    final ActivityAction step3 = new ActivityAction("step3", "Activity Z", "1.0");


    /** Start the workflow by submitting it to SWF. */
    public SimpleWorkflowSequenced() {
        super("Simple Workflow Sequenced", "1.0");

        // This step registers the steps with the workflow so that you don't manually have to
        // inject their workflow, history, state with each call to decide()
        addActions(step1, step2, step3);
    }

    @Override
    public void decide(List<Decision> decisions) {
        String input = getWorkflowInput();
        ActionFn f1 = () -> step1.withInput(input);
        ActionFn f2 = () -> step2.withInput(step1.getOutput());
        ActionFn f3 = () -> step3.withInput(step2.getOutput()).withCompleteWorkflowOnSuccess();

        DecisionBuilder b = new DecisionBuilder(decisions);
        b.sequence(f1, f2, f3);


        sequence(decisions, f1, f2, f3);
    }

}
