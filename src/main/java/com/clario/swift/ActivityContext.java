package com.clario.swift;

import com.amazonaws.services.simpleworkflow.model.RecordActivityTaskHeartbeatRequest;
import com.amazonaws.services.simpleworkflow.model.ScheduleActivityTaskDecisionAttributes;

/**
 * Context passed to methods annotated with {@link ActivityMethod} facilitating
 * getting the activity input and long-running activity tasks that need to record heartbeats.
 *
 * @author George Coller
 */
public interface ActivityContext {

    /**
     * Workflow-unique identifier for this activity task.
     * Useful for logging.
     */
    String getActionId();

    /**
     * Record a heartbeat on SWF for this activity task.
     *
     * @param details optional task progress information.
     *
     * @see RecordActivityTaskHeartbeatRequest#details
     */
    void recordHeartbeat(String details);

    /**
     * Value provided when this activity task was scheduled.
     *
     * @see ScheduleActivityTaskDecisionAttributes#input
     */
    String getInput();
}