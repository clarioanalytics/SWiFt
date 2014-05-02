package com.clario.swift.action;

import com.clario.swift.ActionHistoryEvent;

/**
 * Enumeration of Swift run states for an action.
 * <p/>
 * State is calculated using the most-recent {@link ActionHistoryEvent} related to this action.
 *
 * @see ActionHistoryEvent
 */
public enum ActionState {
    /** Action has not yet been initiated by a workflow, default state of all Actions */
    initial,

    /** Action has been decided and submitted to SWF and is not yet finished. */
    active,

    /** Action retry timer has fired and now should be resubmitted */
    retry,

    /** Action finished successfully on SWF. */
    success,

    /** Action finished in an error state on SWF */
    error
}
