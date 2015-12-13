package com.clario.swift.examples.workflows;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.clario.swift.Workflow;
import com.clario.swift.action.StartChildWorkflowAction;
import com.clario.swift.examples.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * Example workflow that uses a {@link StartChildWorkflowAction} to start a child workflow and keep track of it.
 *
 * @author George Coller
 */
public class StartChildWorkflow extends Workflow {
    public static final Logger log = LoggerFactory.getLogger(StartChildWorkflow.class);

    /** Start the workflow by submitting it to SWF. */
    public static void main(String[] args) {
        Workflow workflow = new StartChildWorkflow()
                                .withDomain(Config.config().getDomain())
                                .withTaskList(Config.config().getTaskList())
                                .withExecutionStartToCloseTimeout(MINUTES, 30)
                                .withTaskStartToCloseTimeout(MINUTES, 30);
        Config.config().submit(workflow, "100");
    }

    //    final RecordMarkerAction childWorkflowIdMarker = new RecordMarkerAction("childWorkflowId");
    private final StartChildWorkflowAction childWorkflow = new StartChildWorkflowAction("childWorkflow")
                                                               .withNameVersion("Simple Workflow", "1.0")
                                                               .withExecutionStartToCloseTimeout(MINUTES, 10)
                                                               .withTaskStartToCloseTimeout(null, -1);

    public StartChildWorkflow() {
        super("Start Child Workflow", "1.0");
        addActions(childWorkflow);
    }

    @Override
    public void decide(List<Decision> decisions) {
        String input = getWorkflowInput();

        if (childWorkflow.withInput(input).decide(decisions).isSuccess()) {
            decisions.add(createCompleteWorkflowExecutionDecision(childWorkflow.getOutput()));
        }
    }
}
