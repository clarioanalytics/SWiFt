package com.clario.swift;

/**
 * Enumeration of possible states a given task can be in.
 *
 * @author George Coller
 */
public enum TaskState {
    /**
     * Ready to be decided.
     */
    initial,

    /**
     * Task has been decided, waiting for it to finish.
     */
    decided,

    /**
     * Task finished successfully.
     */
    finish_ok,

    /**
     * Task finished in an error state.
     */
    finish_error;

    /**
     * @return true if this state is one of {@link #finish_ok} or {@link #finish_error}
     */
    public boolean isFinished() { return this == finish_ok || this == finish_error;}

    /**
     * @return true if this state is {@link #finish_error}
     */
    public boolean isError() { return this == finish_error;}
}
