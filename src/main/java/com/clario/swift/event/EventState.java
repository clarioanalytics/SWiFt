package com.clario.swift.event;

/**
 * State for a workflow, decision, or action in SWF is determined by the {@link EventState} of the most recent
 * {@link Event} related to that task.
 */
public enum EventState {

    /**
     * No events exist for the task.
     */
    NOT_STARTED,

    /**
     * Initial event that started a task.
     */
    INITIAL,

    /**
     * Event representing an ongoing task.
     */
    ACTIVE,

    /**
     * Event representing a task that needs to be retried.
     */
    RETRY,

    /**
     * Event representing a task that has completed successfully.
     */
    SUCCESS,

    /**
     * Event representing a task that has failed.
     */
    ERROR
}
