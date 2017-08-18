package com.clario.swift.action;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.clario.swift.Workflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Flavor of iterating {@link ActivityAction} where the output of the prior iteration is used as the input of the next iteration.
 * </p>
 * <ul>
 * <li>The first iteration will use the value passed with {@link #withInput(String)}</li>
 * <li>Subsequent iterations will use the value from {@link #getOutput()}</li>
 * <li>Use {@link #withOnSuccessRetryPolicy(RetryPolicy)} to describe the polling frequency, max iterations, timeouts, etc</li>
 * <li>Set a {@link RetryPolicyTerminator} on the <pre>RetryPolicy</pre> described above to decide when to terminate the feedback loop</li>
 * </ul>
 *
 * @author George Coller
 */
public class FeedbackActivityAction extends ActivityAction {
    private static final Logger log = LoggerFactory.getLogger(FeedbackActivityAction.class);

    public FeedbackActivityAction(String actionId) {
        super(actionId);
    }

    public FeedbackActivityAction(String actionId, String name, String version) {
        super(actionId, name, version);
    }

    /**
     * If true, this activity will be called with {@link #getInput()} the first time it is invoked and {@link #getOutput()} for any following invocations.
     * </p>
     *
     * @see #withOnSuccessRetryPolicy(RetryPolicy) for setting up a policy to repeat this action.
     */
    @Override
    public Decision createInitiateActivityDecision() {
        String activityTaskInput = isNotStarted() ? getInput() : getOutput();
        if (activityTaskInput == null) {
            log.warn("{} null output, possible if decision triggered before initial activity completed", this);
            return Workflow.createRecordMarkerDecision(this + " null output on decision", "");
        } else {
            return newInitiateActivityDecsion(activityTaskInput);
        }
    }
}
