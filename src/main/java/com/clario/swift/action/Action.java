package com.clario.swift.action;

import com.amazonaws.services.simpleworkflow.model.CancelTimerDecisionAttributes;
import com.amazonaws.services.simpleworkflow.model.Decision;
import com.amazonaws.services.simpleworkflow.model.DecisionType;
import com.clario.swift.DecisionPoller;
import com.clario.swift.EventList;
import com.clario.swift.TaskType;
import com.clario.swift.Workflow;
import com.clario.swift.event.Event;
import com.clario.swift.event.EventState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.amazonaws.services.simpleworkflow.model.EventType.*;
import static com.clario.swift.SwiftUtil.*;
import static com.clario.swift.Workflow.createCompleteWorkflowExecutionDecision;
import static com.clario.swift.Workflow.createFailWorkflowExecutionDecision;
import static com.clario.swift.event.EventState.*;
import static java.lang.String.format;

/**
 * Combines the concepts of SWF Activities, Signals, Child Workflows, and Timers and their current running state.
 * <p/>
 * Note: The name "Action" was chosen to avoid naming conflicts with the parallel SWF concept "Task".
 *
 * @author George Coller
 */
public abstract class Action<T extends Action> {

    private final Logger log;

    private final String actionId;

    private Workflow workflow;
    private RetryPolicy errorRetryPolicy;
    private RetryPolicy successRetryPolicy;
    private boolean failWorkflowOnError = true;
    private boolean completeWorkflowOnSuccess = false;
    private boolean cancelActiveRetryTimer = false;

    /**
     * Each action requires a workflow-unique identifier.
     *
     * @param actionId workflow-unique identifier.
     */
    public Action(String actionId) {
        this.actionId = assertSwfValue(assertMaxLength(actionId, MAX_ID_LENGTH));
        log = LoggerFactory.getLogger(format("%s '%s'", getClass().getSimpleName(), getActionId()));
    }

    public abstract TaskType getTaskType();

    //
    // COMMON GETTERS ACROSS ACTIONS
    //

    /**
     * @return earliest {@link EventState#INITIAL} event's input value, otherwise null
     */
    public String getInput() {
        Event success = getTaskEvents().selectEventState(EventState.INITIAL).getLast();
        return success == null ? null : success.getInput();
    }

    /**
     * @return earliest {@link EventState#INITIAL} event's control value, otherwise null
     */
    public String getControl() {
        Event success = getTaskEvents().selectEventState(EventState.INITIAL).getLast();
        return success == null ? null : success.getControl();
    }

    /**
     * @return most recent {@link #getTaskEvents()} output value if its state is {@link EventState#SUCCESS}, otherwise null.
     */
    public String getOutput() {
        Event event = getTaskEvents().getFirst();
        return (event != null && event.getState() == EventState.SUCCESS) ? event.getOutput() : null;
    }

    /**
     * @return most recent {@link #getTaskEvents()} reason value if its state is {@link EventState#ERROR}, otherwise null.
     */
    public String getReason() {
        Event event = getTaskEvents().getFirst();
        return (event != null && event.getState() == EventState.ERROR) ? event.getReason() : null;
    }

    /**
     * @return most recent {@link #getTaskEvents()} details value if its state is {@link EventState#ERROR}, otherwise null.
     */
    public String getDetails() {
        Event event = getTaskEvents().getFirst();
        return (event != null && event.getState() == EventState.ERROR) ? event.getDetails() : null;
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
     * if this action finishes in an {@link EventState#ERROR} state.
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
     * if this action finishes in an {@link EventState#SUCCESS} state.
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
     * Sets this instance to be retried if the action errors using the given {@link RetryPolicy}.
     * <p/>
     * NOTE: unsupported on {@link TimerAction}.
     */
    public T withOnErrorRetryPolicy(RetryPolicy retryPolicy) {
        this.errorRetryPolicy = retryPolicy;
        if (retryPolicy != null) {
            this.errorRetryPolicy.validate();
            assertRetryPolicySettings();
        }
        return thisObject();
    }

    /**
     * Sets this instance to be rescheduled after each successful completion after a delay determined by the given {@link RetryPolicy}.
     * <p/>
     * NOTE: unsupported on {@link TimerAction}.
     */
    public T withOnSuccessRetryPolicy(RetryPolicy retryPolicy) {
        this.successRetryPolicy = retryPolicy;
        if (retryPolicy != null) {
            this.successRetryPolicy.validate();
            assertRetryPolicySettings();
        }
        return thisObject();
    }

    private void assertRetryPolicySettings() {
        if (errorRetryPolicy != null && successRetryPolicy != null && successRetryPolicy.getControl().equals(errorRetryPolicy.getControl())) {
            throw new IllegalStateException("Assignment of same RetryPolicy instance to an Action on success and on error not allowed");
        }
    }

    /**
     * Add a {@link DecisionType#CancelTimer} decision to the next call to {@link #decide}, which
     * will cancel an active retry timer (if one is currently in progress) for this action.
     * <p/>
     * Useful as a way to cancel a long delay time and force the retry immediately.
     * One scenario could be a workflow with an activity that runs every hour but allows for
     * receiving an external signal that instead kicks off the activity immediately.
     */
    public void withCancelActiveRetryTimer() {
        cancelActiveRetryTimer = true;
    }

    /**
     * Create a {@link DecisionType#CancelTimer} decision that will cancel any active retry timer
     * for this action.
     *
     * @see #withCancelActiveRetryTimer()
     */
    public Decision createCancelRetryTimerDecision() {
        return new Decision()
            .withDecisionType(DecisionType.CancelTimer)
            .withCancelTimerDecisionAttributes(new CancelTimerDecisionAttributes()
                .withTimerId(getActionId())
            );
    }


    /**
     * Make a decision based on the current {@link EventState} of an action.
     * <p/>
     * Default implementation if {@link EventState} is:
     * <ul>
     * <li>{@link EventState#NOT_STARTED}: add decision returned by {@link #createInitiateActivityDecision()}</li>
     * <li>{@link EventState#RETRY}: retry has been activated, add decision returned by {@link #createInitiateActivityDecision()}</li>
     * <li>{@link EventState#ACTIVE}: no decisions are added for in-progress actions</li>
     * <li>{@link EventState#SUCCESS}: if {@link #withNoFailWorkflowOnError()} has previously been called
     * add decision returned by {@link Workflow#createCompleteWorkflowExecutionDecision}</li>
     * <li>{@link EventState#ERROR}: add decision returned by {@link Workflow#createFailWorkflowExecutionDecision}
     * unless {@link #withNoFailWorkflowOnError} has previously been called on the activity</li>
     * </ul>
     *
     * @param decisions decide adds zero or more decisions to this list
     *
     * @see #withNoFailWorkflowOnError
     */
    public Action decide(List<Decision> decisions) {
        EventState eventState = getState();
        Event currentEvent = getCurrentEvent();

        if (cancelActiveRetryTimer && currentEvent != null && TimerStarted == currentEvent.getType()) {
            decisions.add(createCancelRetryTimerDecision());
            eventState = RETRY;
        }
        cancelActiveRetryTimer = false;

        switch (eventState) {
            case NOT_STARTED:
                decisions.add(createInitiateActivityDecision());
                break;
            case INITIAL:
                break;
            case ACTIVE:
                break;
            case SUCCESS:
                boolean isActionCompleted = true;
                if (successRetryPolicy != null) {
                    if (successRetryPolicy.testStopRetrying(getOutput())) {
                        if (isCurrentEventInThisDecision()) {
                            log.info("success retry, terminated");
                        }
                    } else {
                        Decision decision = successRetryPolicy.calcNextDecision(getActionId(), getEvents());
                        if (decision == null) {
                            if (isCurrentEventInThisDecision()) {
                                log.debug("success retry, no more attempts");
                            }
                        } else {
                            decisions.add(decision);
                            isActionCompleted = false;
                            workflow.pushDummyTimerStartedEvent(actionId);
                            log.info("success retry, start timer delay");
                        }
                    }
                }
                if (isActionCompleted) {
                    // Log complete once
                    if (isCurrentEventInThisDecision()) {
                        log.info("action completed");
                    }
                    if (completeWorkflowOnSuccess) {
                        decisions.add(createCompleteWorkflowExecutionDecision(getOutput()));
                    }
                }
                break;
            case RETRY:
                log.info("retry, restart action");
                decisions.add(createInitiateActivityDecision());
                break;

            case ERROR:
                boolean isFailWorkflow = failWorkflowOnError;
                if (errorRetryPolicy != null) {
                    if (errorRetryPolicy.testStopRetrying(currentEvent.getReason()) || errorRetryPolicy.testStopRetrying(currentEvent.getDetails())) {
                        if (isCurrentEventInThisDecision()) {
                            log.info("error retry, terminated");
                        }
                    } else {
                        Decision decision = errorRetryPolicy.calcNextDecision(getActionId(), getEvents());
                        if (decision != null) {
                            decisions.add(decision);
                            isFailWorkflow = false;
                            workflow.pushDummyTimerStartedEvent(actionId);
                            log.info("error retry, start timer delay");
                        } else {
                            if (isCurrentEventInThisDecision()) {
                                log.info("error retry, no more attempts: error={} detail={}", currentEvent.getReason(), currentEvent.getDetails());
                            }
                        }
                    }
                }
                if (isFailWorkflow) {
                    decisions.add(createFailWorkflowExecutionDecision(toString(), currentEvent.getReason(), currentEvent.getDetails()));
                }
                break;
            default:
                throw new IllegalStateException(format("%s unknown action state: %s", this, getState()));
        }
        return this;
    }

    /**
     * Return true, if the current event is part of the events seen since last decision made.
     * Useful as a way to perform certain code once (logging)
     */
    private boolean isCurrentEventInThisDecision() {
        return workflow.getEvents().selectSinceLastDecision().contains(getCurrentEvent());
    }

    /**
     * @return current state for this action.
     * @see EventState for details on how state is calculated
     */
    public EventState getState() {
        Event currentEvent = getCurrentEvent();
        if (currentEvent == null) {
            return NOT_STARTED;
        } else if (TimerStarted == currentEvent.getType() || TimerFired == currentEvent.getType() || TimerCanceled == currentEvent.getType()) {
            return RETRY;
        } else {
            return currentEvent.getState();
        }
    }

    /**
     * Return if action completed with state {@link EventState#SUCCESS}.
     * Can be used in workflows to simply flow logic.  See Swift example workflows.
     */
    public boolean isSuccess() { return SUCCESS == getState(); }

    /**
     * @return true if the action completed with state {@link EventState#ERROR} or {@link EventState#SUCCESS}.
     */
    public boolean isComplete() { return isSuccess() || isError(); }

    /**
     * @return true if this action has not yet been started.
     */
    public boolean isNotStarted() { return NOT_STARTED == getState(); }

    /**
     * @return true if the action completed with state {@link EventState#ERROR}.
     */
    public boolean isError() { return ERROR == getState(); }

    /**
     * Most recently polled {@link Event} for this action
     * or null if none exists (action is in an initial state).
     *
     * @return most recent history event polled for this action.
     */
    @SuppressWarnings("unchecked")
    public <E extends Event> E getCurrentEvent() {
        return getEvents().getFirst();
    }

    /**
     * @return Workflow {@link EventList} selected by {@link #getActionId()} && {@link #getTaskType()}
     */
    public EventList getTaskEvents() {
        return getEvents().selectTaskType(getTaskType());
    }

    /**
     * @return Workflow {@link EventList} selected by {@link #getActionId()}
     */
    public EventList getEvents() {
        assertWorkflowSet();
        return workflow.getEvents().selectActionId(actionId);
    }

    /**
     * @return True if this activity will issue a fail workflow decision on error, otherwise false.
     * @see #withNoFailWorkflowOnError()
     */
    public boolean isFailWorkflowOnError() { return failWorkflowOnError; }

    /**
     * @return true if this activity will issue a complete workflow decision on success, otherwise false.
     * @see #withCompleteWorkflowOnSuccess()
     */
    public boolean isCompleteWorkflowOnSuccess() { return completeWorkflowOnSuccess; }

    /**
     * Subclass implements to create the specific {@link Decision} that initiates the action.
     */
    public abstract Decision createInitiateActivityDecision();

    Logger getLog() { return log; }

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

    private void assertWorkflowSet() {
        if (workflow == null) {
            throw new IllegalStateException(format("%s has no associated workflow. Ensure all actions used by a workflow are added to the workflow.", toString()));
        }
    }
}
