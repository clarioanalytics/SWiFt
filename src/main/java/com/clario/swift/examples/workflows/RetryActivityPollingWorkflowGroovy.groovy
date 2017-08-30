package com.clario.swift.examples.workflows

import com.amazonaws.services.simpleworkflow.model.Decision
import com.clario.swift.GroovyDecisionBuilder
import com.clario.swift.Workflow
import com.clario.swift.action.ActivityAction
import com.clario.swift.action.FeedbackActivityAction
import com.clario.swift.action.RetryPolicy

import java.util.concurrent.TimeUnit

import static com.clario.swift.examples.Config.config
import static java.util.concurrent.TimeUnit.MINUTES

/**
 * @author George Coller
 */
class RetryActivityPollingWorkflowGroovy extends Workflow {

    /** Start the workflow by submitting it to SWF. */
    static void main(String[] args) {
        Workflow workflow = new RetryActivityPollingWorkflowGroovy()
            .withDomain(config().getDomain())
            .withTaskList(config().getTaskList())
            .withTaskStartToCloseTimeout(MINUTES, 60)
            .withExecutionStartToCloseTimeout(MINUTES, 60)
        config().submit(workflow, "120")
    }

    private final ActivityAction init = new ActivityAction("init", "Activity Init", "1.0")

    private final ActivityAction poll = new FeedbackActivityAction("poll", "Activity Poll", "1.0")
        .withOnSuccessRetryPolicy(new RetryPolicy("retry poll")
        .withFixedRetryInterval(TimeUnit.SECONDS, 2)
        .withRetryTerminator({ String output -> output.length() > 8 }) // terminate when the output string is > 8 characters
    )

    private final ActivityAction email = new ActivityAction("email", "Activity Echo", "1.0")

    RetryActivityPollingWorkflowGroovy() {
        super("Retry Activity Polling Workflow", "1.0")
        addActions(init, poll, email)
    }

    @Override
    void decide(List<Decision> decisions) {
        new GroovyDecisionBuilder(decisions)
            .sequence(
            { init.withInput("INIT") },
            { poll.withInput(init.output) })
            .withCompleteWorkflowExecution({ poll.getOutput() })
            .andFinally({ email.withInput("Mock Email") })
            .decide()
    }
}
