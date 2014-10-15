package com.clario.swift.event;

import com.clario.swift.action.Action;

/**
 * Tasks in SWF are represented by one or more related {@link Event} in history with the most recent event
 * representing the current state of the task.
 * <p/>
 * This enumeration makes working with state across different task types easier.
 */
public enum EventState {

    /** State representing an {@link Action} that has not been scheduled (has no history events). */
    INITIAL,

    /** History event representing an {@link Action} that has started and is not yet complete. */
    ACTIVE,

    /** History event representing an {@link Action} that should be re-scheduled. */
    RETRY,

    /** History event representing an {@link Action} that finished in a successful state. */
    SUCCESS,

    /** History event representing an {@link Action} that finished in an error state. */
    ERROR,

    /** History event representing an error on SWF that will immediately fail the workflow */
    CRITICAL,

    /** Informational history event that, not usually useful in decision making */
    INFO
}
