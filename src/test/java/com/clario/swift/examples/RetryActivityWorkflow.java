package com.clario.swift.examples;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.clario.swift.Workflow;
import com.clario.swift.action.RetryPolicy;
import com.clario.swift.action.ActivityAction;
import com.clario.swift.action.RetryPolicy;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.clario.swift.examples.Config.*;
import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * @author George Coller
 */
public class RetryActivityWorkflow extends Workflow {
    private static final Logger log = LoggerFactory.getLogger(RetryActivityWorkflow.class);

    public static void main(String[] args) {
        Workflow workflow = new RetryActivityWorkflow()
            .withDomain(SWIFT_DOMAIN)
            .withTaskList(SWIFT_TASK_LIST)
            .withTaskStartToCloseTimeout(MINUTES, 1)
            .withExecutionStartToCloseTimeout(MINUTES, 5);
        submit(workflow, "30");
    }

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


    public RetryActivityWorkflow() {
        super("Retry Activity Workflow", "1.0");
        addActions(step1);
    }

    @Override
    public void decide(List<Decision> decisions) {

        // Only calculate failUntilTime once
        String failUntilTime = workflowHistory.getMarkers().get("failUntilTime");
        if (failUntilTime == null) {
            int seconds = Integer.parseInt(getWorkflowInput());
            failUntilTime = String.valueOf(new DateTime().plusSeconds(seconds).getMillis());
            decisions.add(createRecordMarkerDecision("failUntilTime", failUntilTime));
        }

        if (step1.withInput(failUntilTime).decide(decisions).isSuccess()) {
            int times = step1.getRetryCount();
            log.info("Activity succeeded after " + times + " times.");
            decisions.add(createCompleteWorkflowExecutionDecision("finished ok!"));
        }
    }
}
