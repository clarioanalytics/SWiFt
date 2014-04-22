package com.clario.swift.action;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.clario.swift.SwfHistoryEvent;
import com.clario.swift.Workflow;

import java.util.List;

import static com.clario.swift.SwiftUtil.createFailWorkflowExecutionDecision;
import static com.clario.swift.action.SwfAction.ActionState.*;
import static java.lang.String.format;

/**
 * Base class for Actions performed in a {@link com.clario.swift.Workflow}.
 * <p/>
 * Note: "Action" was chosen to avoid naming conflicts with SWF's use of "Activity".
 *
 * @author George Coller
 */
public abstract class SwfAction {

    /**
     * Enumeration of possible states a given action can be in.
     *
     * @author George Coller
     */
    public static enum ActionState {
        /** Action has not yet been started on SWF. */
        initial,

        /** Action has been previously decided and submitted to SWF but is not finished. */
        started,

        /** Action finished successfully on SWF. */
        finish_ok,

        /** Action finished in an error state on SWF */
        finish_error;
    }

    protected final String id;
    protected Workflow workflow;
    protected boolean failWorkflowOnError = true;

    public SwfAction(String id) {
        this.id = id;
    }

    public SwfAction withFailWorkflowOnError(boolean value) {
        failWorkflowOnError = value;
        return this;
    }

    /**
     * Main decision method subclasses implement to perform decisions for the action.
     *
     * @param decisions list to add decisions
     *
     * @return true, if action is finished else false.
     */
    public boolean decide(List<Decision> decisions) {
        switch (getState()) {
            case initial:
                decisions.add(createDecision());
                return false;
            case started:
                return false;
            case finish_ok:
                return true;
            case finish_error:
                if (failWorkflowOnError) {
                    decisions.add(createFailWorkflowExecutionDecision(format("%s '%s' error", getClass().getSimpleName(), id), null));
                    return false;
                } else {
                    return true;
                }
            default:
                throw new IllegalStateException("Unknown action state:" + getState());
        }
    }

    public boolean isFinished() {
        return finish_ok == getState() || finish_error == getState();
    }

    public void setWorkflow(Workflow workflow) {
        this.workflow = workflow;
    }

    public ActionState getState() {
        SwfHistoryEvent swfHistoryEvent = getCurrentActionHistoryEvent();
        return swfHistoryEvent == null ? initial : swfHistoryEvent.getActionState();
    }

    public SwfHistoryEvent getCurrentActionHistoryEvent() {
        List<SwfHistoryEvent> events = getActionHistoryEvents();
        return events.isEmpty() ? null : events.get(0);
    }

    public List<SwfHistoryEvent> getActionHistoryEvents() {
        return workflow.getSwfHistory().actionEvents(id);
    }

    protected abstract Decision createDecision();


    protected static void assertTrue(boolean b, String msg) {
        if (!b) {
            throw new AssertionError(msg);
        }
    }

    protected static void assertStringLength(String s, int min, int max, String msg) {
        if (s != null && s.length() < min || s.length() > max) {
            throw new AssertionError(format("%s: between %d and %d", msg, min, max));
        }
    }


    @Override
    public boolean equals(Object o) {
        return this == o || !(o == null || o instanceof SwfAction) && id.equals(((SwfAction) o).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return id;
    }
}
