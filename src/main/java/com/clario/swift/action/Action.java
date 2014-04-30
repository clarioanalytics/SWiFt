package com.clario.swift.action;

import com.amazonaws.services.simpleworkflow.flow.common.FlowConstants;
import com.amazonaws.services.simpleworkflow.flow.interceptors.ExponentialRetryPolicy;
import com.amazonaws.services.simpleworkflow.model.Decision;
import com.amazonaws.services.simpleworkflow.model.DecisionType;
import com.amazonaws.services.simpleworkflow.model.EventType;
import com.clario.swift.ActionHistoryEvent;
import com.clario.swift.DecisionPoller;
import com.clario.swift.Workflow;
import com.clario.swift.WorkflowHistory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.amazonaws.services.simpleworkflow.model.EventType.TimerFired;
import static com.clario.swift.SwiftUtil.*;
import static com.clario.swift.Workflow.createCompleteWorkflowExecutionDecision;
import static com.clario.swift.Workflow.createFailWorkflowExecutionDecision;
import static com.clario.swift.action.Action.State.*;
import static java.lang.String.format;

/**
 * Combines the concepts of SWF Activities, Signals, Child Workflows, and Timers and their current running state.
 * <p/>
 * Note: The name "Action" was chosen to avoid naming conflicts with the parallel SWF concept "Task".
 *
 * @author George Coller
 */
public abstract class Action<T extends Action> {

    /** String used as control value on start timer events to mark them as 'retry' timers. */
    public static final String RETRY_CONTROL_VALUE = "--  SWiFt Retry Control Value --";
    public final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Enumeration of Swift run states for an action.
     * <p/>
     * State is calculated using the most-recent {@link ActionHistoryEvent} related to this action.
     *
     * @see ActionHistoryEvent
     */
    public static enum State {
        /** Action has not yet been initiated by a workflow, default state of all Actions */
        initial,

        /** Action has been decided and submitted to SWF and is not yet finished. */
        active,

        /** Action will be retried */
        retry,

        /** Action finished successfully on SWF. */
        success,

        /** Action finished in an error state on SWF */
        error
    }

    private final String actionId;
    private Workflow workflow;
    private ExponentialRetryPolicy retryPolicy;
    private boolean failWorkflowOnError = true;
    private boolean completeWorkflowOnSuccess = false;

    /**
     * Each action requires a workflow-unique identifier.
     *
     * @param actionId workflow-unique identifier.
     */
    public Action(String actionId) {
        this.actionId = assertSwfValue(assertMaxLength(actionId, MAX_ID_LENGTH));
    }

    /**
     * @return the workflow-unique identifier given at construction
     */
    public String getActionId() { return actionId; }

    /**
     * Called by {@link DecisionPoller} to set the current state for a workflow run.
     */
    public void setWorkflow(Workflow workflow) { this.workflow = workflow; }

    /**
     * @return current state of a workflow run.
     * @see #setWorkflow
     */
    public Workflow getWorkflow() { return workflow; }

    /**
     * @return true, if should decide fail workflow if this action errors.
     * @see #withFailWorkflowOnError()
     */
    public boolean isFailWorkflowOnError() {
        return failWorkflowOnError;
    }

    /**
     * By default {@link #decide} adds a @link DecisionType#FailWorkflowExecution} decision
     * if this action finishes in an {@link State#error} state.
     * <p/>
     * Calling this method deactivates that decision for use cases where a workflow can
     * continue even if this action fails.
     */
    public T withFailWorkflowOnError() {
        failWorkflowOnError = false;
        return (T) this;
    }

    /**
     * @return true, if should decide complete workflow if this action succeeds.
     * @see #withCompleteWorkflowOnSuccess()
     */
    public boolean isCompleteWorkflowOnSuccess() {
        return completeWorkflowOnSuccess;
    }

    /**
     * Calling this will activate a {@link DecisionType#CompleteWorkflowExecution} decision
     * if this action finishes in an {@link State#success} state.
     * <p/>
     * By default this behavior is deactivated but is useful to create final actions in a workflow.
     *
     * @see #completeWorkflowOnSuccess
     */
    public T withCompleteWorkflowOnSuccess() {
        completeWorkflowOnSuccess = true;
        return (T) this;
    }

    // TODO: Figure out how they do exception filtering
    public T withExponentialRetry(ExponentialRetryPolicy retryPolicy) {
        this.retryPolicy = retryPolicy;
        if (retryPolicy != null) {
            retryPolicy.validate();
        }
        return (T) this;
    }

    /**
     * Return output of action.
     *
     * @return result of action, null if action produces no output
     * @throws IllegalStateException if {@link #isSuccess()} is false.
     */
    public String getOutput() {
        if (isSuccess()) {
            String result = getCurrentHistoryEvent().getResult();
            return result;
        } else {
            throw new IllegalStateException("method not available when action state is " + getState());
        }
    }

    /**
     * Make a decision based on the current {@link State} of an action.
     * <p/>
     * Default implementation if {@link State} is:
     * <ul>
     * <li>{@link State#initial}: add decision returned by {@link #createInitiateActivityDecision()}</li>
     * <li>{@link State#retry}: retry has been activated, add decision returned by {@link #createInitiateActivityDecision()}</li>
     * <li>{@link State#active}: no decisions are added for in-progress actions</li>
     * <li>{@link State#success}: if {@link #withFailWorkflowOnError()} has previously been called
     * add decision returned by {@link Workflow#createCompleteWorkflowExecutionDecision}</li>
     * <li>{@link State#error}: add decision returned by {@link Workflow#createFailWorkflowExecutionDecision}
     * unless {@link #withFailWorkflowOnError} has previously been called on the activity</li>
     * </ul>
     *
     * @param decisions decide adds zero or more decisions to this list
     *
     * @see #withFailWorkflowOnError
     */
    public Action decide(List<Decision> decisions) {
        switch (getState()) {
            case initial:
                decisions.add(createInitiateActivityDecision());
                break;
            case active:
                break;
            case success:
                log.info(format("%s completed", this));
                if (completeWorkflowOnSuccess) {
                    decisions.add(createCompleteWorkflowExecutionDecision(getOutput()));
                }
                break;
            case retry:
                log.info(format("%s decide retry", this));
                decisions.add(createInitiateActivityDecision());
                break;

            case error:
                if (retryPolicy != null) {
                    long nextDelayInSeconds = calcNextDelayInSeconds();
                    String reason = getCurrentHistoryEvent().getErrorReason();
                    if (FlowConstants.NONE == nextDelayInSeconds) {
                        log.info(format("%s error, retry policy has no more attempts. reason:%s", this, reason));
                    } else {
                        TimerAction timer = new TimerAction(getActionId())
                            .withStartToFireTimeout(TimeUnit.SECONDS, nextDelayInSeconds)
                            .withControl(RETRY_CONTROL_VALUE);
                        log.info(format("%s error, will retry after %d seconds. reason:%s", this, nextDelayInSeconds, reason));
                        decisions.add(timer.createInitiateActivityDecision());
                        break;
                    }
                }
                if (failWorkflowOnError) {
                    decisions.add(createFailWorkflowExecutionDecision(format("%s error, decide fail workflow", this), null));
                }

                break;
            default:
                throw new IllegalStateException(format("%s unknown action state:%s", this, getState()));
        }
        return this;
    }

    /**
     * @see ExponentialRetryPolicy#nextRetryDelaySeconds(java.util.Date, java.util.Date, int)
     */
    protected long calcNextDelayInSeconds() {
        List<ActionHistoryEvent> retries = getRetryTimerStartedEvents();
        if (retries.isEmpty()) {
            return retryPolicy.getInitialRetryIntervalSeconds();
        } else {
            int numberOfTries = retries.size() + 1;
            Date firstAttempt = retries.get(retries.size() - 1).getEventTimestamp();
            Date recordedFailure = getCurrentHistoryEvent().getEventTimestamp();
            return retryPolicy.nextRetryDelaySeconds(firstAttempt, recordedFailure, numberOfTries);
        }
    }

    /**
     * Return {@link EventType#TimerStarted} events that were used for retries.
     */
    public List<ActionHistoryEvent> getRetryTimerStartedEvents() {
        return workflow.getWorkflowHistory().filterRetryTimerStartedEvents(getActionId());
    }

    /**
     * @return current state for this action.
     * @see State for details on how state is calculated
     */
    public State getState() {
        ActionHistoryEvent swfHistoryEvent = getCurrentHistoryEvent();
        if (swfHistoryEvent == null) {
            return initial;
        } else if (TimerFired == swfHistoryEvent.getType()) {
            Long eventId = swfHistoryEvent.getInitialEventId();
            for (ActionHistoryEvent event : getRetryTimerStartedEvents()) {
                if (eventId.equals(event.getEventId())) {
                    return retry;
                }
            }
        }
        return swfHistoryEvent.getActionState();
    }

    /**
     * Return if action completed with state {@link State#success}.
     * Can be used in workflows to simply flow logic.  See Swift example workflows.
     */
    public boolean isSuccess() { return success == getState(); }

    /**
     * Most recently polled {@link ActionHistoryEvent} for this action
     * or null if none exists (action is in an initial state).
     *
     * @return most recent history event polled for this action.
     */
    public ActionHistoryEvent getCurrentHistoryEvent() {
        List<ActionHistoryEvent> events = getHistoryEvents();
        return events.isEmpty() ? null : events.get(0);
    }

    /**
     * Return the list of available history events polled for this action.
     * <p/>
     * The list is sorted by {@link ActionHistoryEvent#getEventTimestamp()} in descending order (most recent events first).
     *
     * @return list of events or empty list.
     * @see WorkflowHistory#filterEvents
     */
    public List<ActionHistoryEvent> getHistoryEvents() {
        return workflow.getWorkflowHistory().filterEvents(actionId);
    }

    /**
     * Subclass implements to create the specific {@link Decision} that initiates the action.
     */
    public abstract Decision createInitiateActivityDecision();


    /** Two actions are considered equal if their id is equal. */
    @Override
    public boolean equals(Object o) {
        return this == o || !(o == null || o instanceof Action) && actionId.equals(((Action) o).actionId);
    }

    @Override
    public int hashCode() {
        return actionId.hashCode();
    }

    @Override
    public String toString() {
        return format("%s %s", getClass().getSimpleName(), getActionId());
    }
}
