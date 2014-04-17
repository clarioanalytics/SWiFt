package com.clario.swift;

/**
 * Enumeration of possible states a given task can be in.
 *
 * @author George Coller
 */
public enum TaskState {
    /**
     * Blocked from deciding while waiting for parents to finish.
     * <p/>
     * Initial state for Tasks with one or more parents.
     */
    wait_for_parents,
    /**
     * Ready to decide, all parents have finished successfully.
     * <p/>
     * Initial state for Tasks with zero parents.
     */
    ready_to_decide,
    /**
     * Task has been decided, waiting for it to finish.
     */
    scheduled,
    /**
     * Task finished successfully.
     */
    finish_ok,
    /**
     * Task canceled because it was a canceled Activity one or more of its parents finished with a cancel or error.
     */
    finish_cancel,
    /**
     * Task finished in an error state.
     */
    finish_error;

    /**
     * @return true if this state is one of {@link #finish_ok}, {@link #finish_cancel}, or {@link #finish_error}
     */
    public boolean isFinished() { return this == finish_ok || this == finish_cancel || this == finish_error;}

    /**
     * @return true if this state is one of {@link #finish_cancel}, or {@link #finish_error}
     */
    public boolean isErrorOrCancel() { return this == finish_cancel || this == finish_error;}
}
