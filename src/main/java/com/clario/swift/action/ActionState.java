package com.clario.swift.action;

import com.clario.swift.ActionEvent;

/**
 * Enumeration of run states for an {@link Action}.
 * <p/>
 * An action's current state is calculated using the most recent {@link ActionEvent} in the related to that action.
 *
 * @see ActionEvent
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
    error,

    /** Action state is undefined, used instead of 'null' */
    undefined
}
