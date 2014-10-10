package com.clario.swift.examples.workflows;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.clario.swift.Workflow;
import com.clario.swift.action.Action;
import com.clario.swift.action.ActivityAction;
import com.clario.swift.action.ContinueAsNewAction;
import com.clario.swift.action.RetryPolicy;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.amazonaws.services.simpleworkflow.model.ChildPolicy.TERMINATE;
import static com.clario.swift.SwiftUtil.defaultIfEmpty;
import static com.clario.swift.examples.Config.config;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.joda.time.Minutes.minutesBetween;

/**
 * Example of a cron-like workflow where an activity gets fired every 10 seconds.
 * <p/>
 * Two SWiFt concepts are being used here
 * <ul>
 * <li>{@link ContinueAsNewAction} - action that will terminate the current workflow and restart it</li>
 * <li>{@link Action#withOnSuccessRetryPolicy} - set with a {@link RetryPolicy}, which repeats the action five times<li>
 * </ul>
 * <p/>
 * Notice how the action poller's log shows an increasing counter with every "Activity Echo" call,
 * preserving the state of the workflow even after it's terminated and restarted as new.
 *
 * @author George Coller
 */
public class CronWorkflow extends Workflow {
    private final Logger log = LoggerFactory.getLogger(CronWorkflow.class);

    public static void main(String[] args) {
        Workflow workflow = new CronWorkflow()
            .withDomain(config().getDomain())
            .withTaskList(config().getTaskList())
            .withExecutionStartToCloseTimeout(MINUTES, 5)
            .withTaskStartToCloseTimeout(SECONDS, 30)
            .withChildPolicy(TERMINATE)
            .withDescription("A Simple Example Workflow");
        config().submit(workflow, "0");
    }

    // In a production workflow this would probably be set to once a day or once a week depending a workflow's chattiness
    public static final int CONTINUE_WORKFLOW_AFTER_MINUTES = 1;

    // Define a policy that returns 10 seconds
    private final RetryPolicy repeatEvery10Seconds5Times = new RetryPolicy()
        .withFixedRetryInterval(TimeUnit.SECONDS, 10)
        .withMaximumAttempts(5);

    final ActivityAction echoActivity = new ActivityAction("echo", "Activity Echo", "1.0")
        .withOnSuccessRetryPolicy(repeatEvery10Seconds5Times);

    final ContinueAsNewAction continueAsNewAction = new ContinueAsNewAction("continue");

    public CronWorkflow() {
        super("Cron Workflow", "1.0");
        addActions(echoActivity, continueAsNewAction);
    }

    @Override public void decide(List<Decision> decisions) {
        // Get the state of the workflow
        String echoCount;
        if (echoActivity.isInitial()) {
            echoCount = defaultIfEmpty(getWorkflowInput(), "0");
            log.info("initialize echoCount=" + echoCount);
        } else {
            echoCount = String.valueOf(Integer.valueOf(echoActivity.getOutput()) + 1);
            log.info("increment echoCount=" + echoCount);
        }

        int wfRuntimeMins = minutesBetween(new DateTime(getWorkflowStartDate()), DateTime.now()).getMinutes();
        if (wfRuntimeMins >= CONTINUE_WORKFLOW_AFTER_MINUTES) {
            // workflow has run for over a minute.  continue as new preserving the current echoCount state
            log.info("continue as new workflow with echoCount=" + echoCount);
            continueAsNewAction.withInput(echoCount).decide(decisions);
        } else {
            echoActivity.withInput(echoCount).decide(decisions);
        }
    }
}
