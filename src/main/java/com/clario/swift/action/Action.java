package com.clario.swift.action;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.amazonaws.services.simpleworkflow.model.DecisionType;
import com.amazonaws.services.simpleworkflow.model.FailWorkflowExecutionDecisionAttributes;
import com.clario.swift.ActionHistoryEvent;
import com.clario.swift.DecisionPoller;
import com.clario.swift.Workflow;
import com.clario.swift.WorkflowHistory;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.clario.swift.action.Action.State.initial;
import static com.clario.swift.action.Action.State.success;
import static java.lang.String.format;

/**
 * Combines the concepts of SWF Activities, Signals, Child Workflows, and Timers and their current running state.
 * <p/>
 * Note: The name "Action" was chosen to avoid naming conflicts with the parallel SWF concept "Task".
 *
 * @author George Coller
 */
public abstract class Action {

    public Workflow getWorkflow() {
        return workflow;
    }

    public boolean isFailWorkflowExecution() {
        return failWorkflowExecution;
    }

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

        /** Action finished successfully on SWF. */
        success,

        /** Action finished in an error state on SWF */
        error
    }

    private final String actionId;
    private boolean failWorkflowExecution = true;
    private Workflow workflow;

    /**
     * Each action requires a workflow-unique identifier.
     *
     * @param actionId workflow-unique identifier.
     */
    public Action(String actionId) {
        this.actionId = actionId;
    }

    /**
     * By default {@link #decide} adds a @link DecisionType#FailWorkflowExecution} decision
     * if this action finishes in an {@link State#error} state.
     * <p/>
     * Calling this method deactivates that decision for use cases where a workflow can
     * continue even if this action fails.
     */
    public Action withDeactivateFailWorkflowExecution() {
        failWorkflowExecution = false;
        return this;
    }

    /**
     * Make a decision based on the current {@link State} of an action.
     * <p/>
     * Default implementation if {@link State} is:
     * <ul>
     * <li>{@link State#initial} add decision returned by {@link #createInitiateActivityDecision()}</li>
     * <li>{@link State#error} add decision returned by {@link #createFailWorkflowExecutionDecision}
     * unless {@link #withDeactivateFailWorkflowExecution} has previously been called on the activity</li>
     * </ul>
     *
     * @param decisions decide adds zero or more decisions to this list
     *
     * @see #withDeactivateFailWorkflowExecution
     */
    public Action decide(List<Decision> decisions) {
        switch (getState()) {
            case initial:
                decisions.add(createInitiateActivityDecision());
                break;
            case active:
                break;
            case success:
                break;
            case error:
                if (failWorkflowExecution) {
                    decisions.add(createFailWorkflowExecutionDecision(format("%s '%s' error", getClass().getSimpleName(), actionId), null));
                }
                break;
            default:
                throw new IllegalStateException("Unknown action state:" + getState());
        }
        return this;
    }

    /**
     * @return the workflow-unique identifier given at construction
     */
    public String getActionId() { return actionId; }

    /**
     * Called by {@link DecisionPoller} to set the current state for a workflow run.
     */
    public void setWorkflow(Workflow workflow) {
        this.workflow = workflow;
    }

    /**
     * @return current state for this action.
     * @see State for details on how state is calculated
     */
    public State getState() {
        ActionHistoryEvent swfHistoryEvent = getCurrentHistoryEvent();
        return swfHistoryEvent == null ? initial : swfHistoryEvent.getActionState();
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
     * The list is sorted by {@link ActionHistoryEvent#eventTimestamp} in descending order (most recent events first).
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

    /**
     * Create a {@link DecisionType#FailWorkflowExecution} decision.
     *
     * @see FailWorkflowExecutionDecisionAttributes for info and field restrictions
     */
    public static Decision createFailWorkflowExecutionDecision(String reason, String details) {
        return new Decision()
            .withDecisionType(DecisionType.FailWorkflowExecution)
            .withFailWorkflowExecutionDecisionAttributes(
                new FailWorkflowExecutionDecisionAttributes()
                    .withReason(reason)
                    .withDetails(details)
            );
    }

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
        return actionId;
    }
}
