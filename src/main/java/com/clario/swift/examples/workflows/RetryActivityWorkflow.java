package com.clario.swift.examples.workflows;

import com.amazonaws.services.simpleworkflow.model.Decision;
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
 * @author George Coller
 * @see RetryPolicy
 */
public class RetryActivityWorkflow extends Workflow {
    private static final Logger log = LoggerFactory.getLogger(RetryActivityWorkflow.class);

    /** Start the workflow by submitting it to SWF. */
    public static void main(String[] args) {
        Workflow workflow = new RetryActivityWorkflow()
            .withDomain(config.getDomain())
            .withTaskList(config.getTaskList())
            .withTaskStartToCloseTimeout(MINUTES, 60)
            .withExecutionStartToCloseTimeout(MINUTES, 60);
        config.submit(workflow, "120");
    }

    // Adding the RetryPolicy is the important part of this example
    private final ActivityAction step1 = new ActivityAction("step1", "Activity Fail Until", "1.0")
        .withScheduleToCloseTimeout(MINUTES, 1)
        .withStartToCloseTimeout(MINUTES, 1)
        .withScheduleToStartTimeout(MINUTES, 1)
        .withHeartBeatTimeoutTimeout(MINUTES, 1)

        .withRetryPolicy(new RetryPolicy()
                .withInitialRetryInterval(TimeUnit.SECONDS, 5)
                .withMaximumRetryInterval(TimeUnit.MINUTES, 1)
                .withRetryExpirationInterval(TimeUnit.HOURS, 1)
                .withMaximumAttempts(20)
        );

    private final RecordMarkerAction failUntilTimeMarker = new RecordMarkerAction("failUntilTime");


    public RetryActivityWorkflow() {
        super("Retry Activity Workflow", "1.0");
        addActions(step1, failUntilTimeMarker);
    }

    @Override
    public void decide(List<Decision> decisions) {

        if (failUntilTimeMarker.isInitial()) {
            // Use marker to do this code exactly once
            int seconds = Integer.parseInt(getWorkflowInput());
            DateTime dateTime = new DateTime().plusSeconds(seconds);
            log.info("Should fail and retry until after: {}", SwiftUtil.DATE_TIME_MILLIS_FORMATTER.print(dateTime));
            failUntilTimeMarker
                .withDetails(String.valueOf(dateTime.getMillis()))
                .decide(decisions);
        }
        String failUntilTime = failUntilTimeMarker.getOutput();

        if (step1.withInput(failUntilTime).decide(decisions).isSuccess()) {
            int times = step1.getRetryCount();
            log.info("Activity succeeded after " + times + " times at " + SwiftUtil.DATE_TIME_MILLIS_FORMATTER.print(DateTime.now()));
            decisions.add(createCompleteWorkflowExecutionDecision("finished ok!"));
        }
    }
}
