package com.clario.swift;

import com.amazonaws.services.simpleworkflow.model.EventType;
import com.amazonaws.services.simpleworkflow.model.HistoryEvent;
import com.clario.swift.action.ActionState;

import java.util.*;

/**
 * Used by {@link Workflow} to hold {@link ActionEvent}s for the current decision.
 * <p/>
 * Most of the heavy lifting comes from converting each SWF {@link HistoryEvent} into a {@link ActionEvent} to
 * unify working with various SWF tasks like activities, timers, signals, starting child workflows.
 * <p/>
 * This class will also find any un-recoverable workflow error events.
 *
 * @author George Coller
 * @see ActionEvent
 * @see Workflow
 */
public class WorkflowHistory {
    private LinkedList<ActionEvent> actionEvents = new LinkedList<ActionEvent>();
    private List<HistoryEvent> errorEvents = new ArrayList<HistoryEvent>();
    private HistoryEvent priorDecisionTaskCompleted;
    private HistoryEvent workflowExecutionStarted;

    public WorkflowHistory() {
    }

    public void addHistoryEvents(List<HistoryEvent> historyEvents) {
        priorDecisionTaskCompleted = null;

        // Note: historyEvents are sorted newest to oldest
        for (HistoryEvent event : historyEvents) {

            ActionEvent actionEvent = new ActionEvent(event);

            // filter out events we don't care about
            if (actionEvent.getActionState() != ActionState.undefined) {
                actionEvents.add(actionEvent);
            }

            switch (actionEvent.getType()) {
                case WorkflowExecutionStarted:
                    workflowExecutionStarted = event;
                    break;

                case DecisionTaskCompleted:
                    if (priorDecisionTaskCompleted == null) {
                        priorDecisionTaskCompleted = event;
                    }
                    break;

                // Events that can't be recovered from, config or state problems, etc.
                case WorkflowExecutionCancelRequested:
                case ScheduleActivityTaskFailed:
                case StartChildWorkflowExecutionFailed:
                case SignalExternalWorkflowExecutionFailed:
                    errorEvents.add(event);
                    break;
            }
        }
    }

    /**
     * Reset instance to prepare for new set of history.
     */
    public void reset() {
        actionEvents = new LinkedList<ActionEvent>();
        errorEvents = new ArrayList<HistoryEvent>();
        workflowExecutionStarted = null;
    }

    /**
     * Return the list of {@link ActionEvent} related to a given action.
     * The list is sorted by event id in descending order (most recent first).
     *
     * @param actionId workflow unique identifier of the action.
     *
     * @return the list, empty if no actions found
     */
    public List<ActionEvent> filterActionEvents(String actionId) {
        List<ActionEvent> list = new ArrayList<ActionEvent>();

        // iterate backwards through list (need to find initial event first)
        Iterator<ActionEvent> iter = actionEvents.descendingIterator();
        long initialId = -1;
        while (iter.hasNext()) {
            ActionEvent event = iter.next();
            if (event.isInitialEvent() && event.getActionId().equals(actionId)) {
                initialId = event.getEventId();
                list.add(event);
            } else if (initialId == event.getInitialEventId()) {
                list.add(event);
            }
        }
        Collections.reverse(list);
        return list;
    }

    /**
     * Filter events that match the given filter parameters.
     *
     * @param actionId optional, unique id of the action.
     * @param eventType optional, event type
     * @param sinceLastDecision if true, return only events received since the last decision (or workflow start if no decisions have been made).
     *
     * @return list of matching events
     */
    public List<ActionEvent> filterEvents(String actionId, EventType eventType, boolean sinceLastDecision) {
        List<ActionEvent> list = new ArrayList<ActionEvent>();
        for (ActionEvent event : actionId == null ? actionEvents : filterActionEvents(actionId)) {
            Long eventId = sinceLastDecision ? getPriorDecisionEventId() : 0;
            if ((eventType == null || event.getType() == eventType) && event.getEventId() > eventId) {
                list.add(event);
            }
        }
        return list;
    }

    /**
     * @param sinceLastDecision if true, return only events received since the last decision (or workflow start if no decisions have been made).
     *
     * @return events with type {@link EventType#MarkerRecorded} converted to a map of marker name, details entries.
     */
    public List<ActionEvent> getMarkers(boolean sinceLastDecision) {
        return filterEvents(null, EventType.MarkerRecorded, sinceLastDecision);
    }

    /**
     * @param sinceLastDecision if true, return only events received since the last decision (or workflow start if no decisions have been made).
     *
     * @return events with type {@link EventType#WorkflowExecutionSignaled} converted to a map of signal name, input entries.
     */
    public List<ActionEvent> getSignals(boolean sinceLastDecision) {
        return filterEvents(null, EventType.WorkflowExecutionSignaled, sinceLastDecision);
    }

    /**
     * If available, return the input string given to this workflow when it was initiated on SWF.
     * <p/>
     * This value will not be available if a workflow's {@link Workflow#isContinuePollingForHistoryEvents()} is
     * implemented, which may stop the poller from receiving all of a workflow run's history events.
     *
     * @return the input or null if not available
     */
    public String getWorkflowInput() {
        if (workflowExecutionStarted == null) {
            return null;
        } else {
            return workflowExecutionStarted.getWorkflowExecutionStartedEventAttributes().getInput();
        }
    }

    /**
     * If available return the start date of the workflow when it was initiated on SWF.
     * <p/>
     * This value will not be available if a workflow's {@link Workflow#isContinuePollingForHistoryEvents()} is
     * implemented, which may stop the poller from receiving all of a workflow run's history events.
     *
     * @return the workflow start date or null if not available
     */
    public Date getWorkflowStartDate() {
        if (workflowExecutionStarted == null) {
            return null;
        } else {
            return workflowExecutionStarted.getEventTimestamp();
        }
    }

    /**
     * Return the date of the most recent completed decision or the workflow start date if none exists.
     * </p>
     * Useful for filtering events created since the last decision.
     *
     * @return date of most recent completed decision or workflow start date
     * @see #getWorkflowStartDate()
     */
    public Date getPriorDecisionDate() {
        if (priorDecisionTaskCompleted != null) {
            return priorDecisionTaskCompleted.getEventTimestamp();
        } else {
            return getWorkflowStartDate();
        }
    }

    /**
     * Return the event Id of the most recent completed decision or the workflow start (0) if none exists.
     * </p>
     * Useful for filtering events created since the last decision.
     *
     * @return date of most recent completed decision or workflow start date
     * @see #getWorkflowStartDate()
     */
    public Long getPriorDecisionEventId() {
        if (priorDecisionTaskCompleted != null) {
            return priorDecisionTaskCompleted.getEventId();
        } else {
            return 1L;
        }
    }

    public HistoryEvent getPriorDecisionTaskCompleted() { return priorDecisionTaskCompleted; }

    public HistoryEvent getWorkflowExecutionStarted() { return workflowExecutionStarted; }

    /**
     * Get any error events recorded for current SWF decision task.
     */
    public List<HistoryEvent> getErrorEvents() {
        return errorEvents;
    }

    public List<ActionEvent> getActionEvents() {
        return actionEvents;
    }
}
