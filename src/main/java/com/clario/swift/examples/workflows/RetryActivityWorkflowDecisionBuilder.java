package com.clario.swift.examples.workflows;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.clario.swift.DecisionBuilder;
import com.clario.swift.SwiftUtil;
import com.clario.swift.Workflow;
import com.clario.swift.action.ActivityAction;
import com.clario.swift.action.RecordMarkerAction;
import com.clario.swift.action.RetryPolicy;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.clario.swift.examples.Config.config;
import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * Example of adding a {@link RetryPolicy} to an action so that it will be retried several times
 * depending on the configurable policy settings.
 *
 * @author George Coller
 * @see RetryPolicy
 */
public class RetryActivityWorkflowDecisionBuilder extends Workflow {
    private static final Logger log = LoggerFactory.getLogger(RetryActivityWorkflowDecisionBuilder.class);

    /** Start the workflow by submitting it to SWF. */
    public static void main(String[] args) {
        Workflow workflow = new RetryActivityWorkflowDecisionBuilder()
            .withDomain(config().getDomain())
            .withTaskList(config().getTaskList())
            .withTaskStartToCloseTimeout(MINUTES, 60)
            .withExecutionStartToCloseTimeout(MINUTES, 60);
        config().submit(workflow, "120");
    }

    // RetryPolicy subclasses should be thread-safe so we can share them
    private final RetryPolicy RETRY_POLICY = new RetryPolicy("step1 retry")
        .withInitialRetryInterval(TimeUnit.SECONDS, 5)
        .withMaximumRetryInterval(TimeUnit.SECONDS, 20)
        .withRetryExpirationInterval(TimeUnit.HOURS, 1)
        .withMaximumAttempts(20);

    // Adding the RETRY_POLICY is the important part of this example
    private final ActivityAction step1 = new ActivityAction("step1", "Activity Fail Until", "1.0")
        .withScheduleToCloseTimeout(MINUTES, 1)
        .withStartToCloseTimeout(MINUTES, 1)
        .withScheduleToStartTimeout(MINUTES, 1)
        .withHeartBeatTimeoutTimeout(MINUTES, 1)
        .withOnErrorRetryPolicy(RETRY_POLICY);

    final ActivityAction step2 = new ActivityAction("step2", "Activity X", "1.0");

    private final RecordMarkerAction failUntilTimeMarker = new RecordMarkerAction("failUntilTime");


    public RetryActivityWorkflowDecisionBuilder() {
        super("Retry Activity Workflow Decision Builder", "1.0");
        addActions(step1, step2, failUntilTimeMarker);
    }

    @Override
    public void decide(List<Decision> decisions) {
        RetryActivityWorkflow.recordFailUntilTimeMarker(failUntilTimeMarker, decisions, getWorkflowInput());

        String failUntilTime = failUntilTimeMarker.getOutput();
        DecisionBuilder d = new DecisionBuilder(decisions);

        d.sequence(() -> step1.withInput(failUntilTime))
            .andFinally(() -> step2.withInput("123"))
            .decide();

        if (step1.isSuccess() && step2.isSuccess()) {
            int times = step1.getEvents().selectRetryCount(RETRY_POLICY.getControl()).size();
            log.info("Activity succeeded after " + times + " times at " + SwiftUtil.DATE_TIME_MILLIS_FORMATTER.print(DateTime.now()));
            decisions.add(createCompleteWorkflowExecutionDecision("finished ok!"));
        }
    }
}
