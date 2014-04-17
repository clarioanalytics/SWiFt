package com.clario.swift;

/**
 * @author George Coller
 */
public enum TaskState {
    wait_for_parents, ready_to_decide, scheduled, finish_cancel, finish_ok, finish_error;

    public boolean isFinished() { return this == finish_ok || this == finish_cancel || this == finish_error;}

    public boolean isErrorOrCancel() { return this == finish_cancel || this == finish_error;}
}
