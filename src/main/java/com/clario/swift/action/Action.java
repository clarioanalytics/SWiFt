package com.clario.swift.action;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.amazonaws.services.simpleworkflow.model.DecisionType;
import com.clario.swift.ActionHistoryEvent;
import com.clario.swift.DecisionPoller;
import com.clario.swift.Workflow;
import com.clario.swift.WorkflowHistory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.clario.swift.SwiftUtil.*;
import static com.clario.swift.Workflow.createCompleteWorkflowExecutionDecision;
import static com.clario.swift.Workflow.createFailWorkflowExecutionDecision;
import static com.clario.swift.action.ActionState.*;
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
    protected final Logger log = LoggerFactory.getLogger(getClass());

    private final String actionId;

    private Workflow workflow;
    private RetryPolicy retryPolicy;
    private boolean failWorkflowOnError = true;
    private boolean completeWorkflowOnSuccess = false;
    private long retryCount;

    /**
     * Each action requires a workflow-unique identifier.
     *
     * @param actionId workflow-unique identifier.
     */
    public Action(String actionId) {
        this.actionId = assertSwfValue(assertMaxLength(actionId, MAX_ID_LENGTH));
    }

    /**
     * Subclass overrides to provide "this" which allows super class to do method chaining
     * without compiler warnings.  Generics, what can you do?
     */
    protected abstract T thisObject();

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
     * By default {@link #decide} adds a @link DecisionType#FailWorkflowExecution} decision
     * if this action finishes in an {@link ActionState#error} state.
     * <p/>
     * Calling this method deactivates that decision for use cases where a workflow can
     * continue even if this action fails.
     */
    public T withNoFailWorkflowOnError() {
        failWorkflowOnError = false;
        return thisObject();
    }

    /**
     * Calling this will activate a {@link DecisionType#CompleteWorkflowExecution} decision
     * if this action finishes in an {@link ActionState#success} state.
     * <p/>
     * By default this behavior is deactivated but is useful to create final actions in a workflow.
     *
     * @see #completeWorkflowOnSuccess
     */
    public T withCompleteWorkflowOnSuccess() {
        completeWorkflowOnSuccess = true;
        return thisObject();
    }

    /**
     * Adding a {@link RetryPolicy} marks this instance to be retried if the action errors.
     * <p/>
     * {@link RetryPolicy#setAction} will be called with this instance and then
     * {@link RetryPolicy#validate}.
     *
     * @param retryPolicy policy, by default there is no retry policy
     *
     * @throws IllegalStateException if policy is not valid
     */
    public T withRetryPolicy(RetryPolicy retryPolicy) {
        this.retryPolicy = retryPolicy;
        if (retryPolicy != null) {
            this.retryPolicy.setAction(this);
            this.retryPolicy.validate();
        }
        return thisObject();
    }

    public int getRetryCount() {
        return retryPolicy == null ? 0 : retryPolicy.getRetryCount();
    }

    /**
     * Return output of action.
     *
     * @return result of action, null if action produces no output
     * @throws IllegalStateException if {@link #isSuccess()} is false.
     */
    public String getOutput() {
        if (isSuccess()) {
            return getCurrentHistoryEvent().getResult();
        } else {
            throw new IllegalStateException("method not available when action state is " + getState());
        }
    }

    /**
     * Make a decision based on the current {@link ActionState} of an action.
     * <p/>
     * Default implementation if {@link ActionState} is:
     * <ul>
     * <li>{@link ActionState#initial}: add decision returned by {@link #createInitiateActivityDecision()}</li>
     * <li>{@link ActionState#retry}: retry has been activated, add decision returned by {@link #createInitiateActivityDecision()}</li>
     * <li>{@link ActionState#active}: no decisions are added for in-progress actions</li>
     * <li>{@link ActionState#success}: if {@link #withNoFailWorkflowOnError()} has previously been called
     * add decision returned by {@link Workflow#createCompleteWorkflowExecutionDecision}</li>
     * <li>{@link ActionState#error}: add decision returned by {@link Workflow#createFailWorkflowExecutionDecision}
     * unless {@link #withNoFailWorkflowOnError} has previously been called on the activity</li>
     * </ul>
     *
     * @param decisions decide adds zero or more decisions to this list
     *
     * @see #withNoFailWorkflowOnError
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
                    String reason = getCurrentHistoryEvent().getErrorReason();
                    int delaySeconds = retryPolicy.nextRetryDelaySeconds();
                    if (delaySeconds >= 0) {
                        TimerAction timer = new TimerAction(getActionId())
                            .withStartToFireTimeout(TimeUnit.SECONDS, delaySeconds)
                            .withControl(RETRY_CONTROL_VALUE);
                        log.info(format("%s error, will retry after %d seconds. reason:%s", this, delaySeconds, reason));
                        decisions.add(timer.createInitiateActivityDecision());
                        break;
                    } else {
                        log.info(format("%s error, retry policy has no more attempts. reason:%s", this, reason));
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
     * @return current state for this action.
     * @see ActionState for details on how state is calculated
     */
    public ActionState getState() {
        ActionHistoryEvent currentEvent = getCurrentHistoryEvent();
        if (currentEvent == null) {
            return initial;
        } else if (retryPolicy != null && retryPolicy.isRetryTimerEvent(currentEvent)) {
            return retry;
        } else {
            return currentEvent.getActionState();
        }
    }

    /**
     * Return if action completed with state {@link ActionState#success}.
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
        return o == this || (o != null && o instanceof Action && actionId.equals(((Action) o).actionId));
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
