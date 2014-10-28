package com.clario.swift;

/**
 * @author George Coller
 */
public enum TaskType {
    ACTIVITY,
    TIMER,
    START_CHILD_WORKFLOW,
    RECORD_MARKER,
    SIGNAL_EXTERNAL_WORKFLOW,
    CANCEL_EXTERNAL_WORKFLOW,
    CONTINUE_AS_NEW,
    DECISION,
    WORKFLOW_EXECUTION,
    WORKFLOW_SIGNALED;

    public static final String CATEGORY_DECISION = "CATEGORY_DECISION";
    public static final String CATEGORY_WORKFLOW = "CATEGORY_WORKFLOW";
    public static final String CATEGORY_SIGNAL = "CATEGORY_SIGNAL";
    public static final String CATEGORY_ACTION = "CATEGORY_ACTION";

    /**
     * Category of task.
     *
     * @return one of {@link #CATEGORY_DECISION}, {@link #CATEGORY_WORKFLOW}, {@link #CATEGORY_SIGNAL}, {@link #CATEGORY_ACTION},
     */
    String getCategory() {
        if (this == DECISION) {
            return CATEGORY_DECISION;
        } else if (this == WORKFLOW_EXECUTION) {
            return CATEGORY_WORKFLOW;
        } else if (this == WORKFLOW_SIGNALED) {
            return CATEGORY_SIGNAL;
        } else {
            return CATEGORY_ACTION;
        }
    }
}
