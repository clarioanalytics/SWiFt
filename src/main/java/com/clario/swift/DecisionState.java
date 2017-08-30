package com.clario.swift;

/**
 * Decision Builder State.
 * @author George Coller
 */
enum DecisionState {
    notStarted, success, error;

    boolean isPending() {
        return this == notStarted;
    }

    boolean isFinished() {
        return this == success || this == error;
    }

    boolean isError() { return this == error; }

    boolean isSuccess() {
        return this == success;
    }
}
