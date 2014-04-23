package com.clario.swift;

import com.amazonaws.services.simpleworkflow.model.RecordActivityTaskHeartbeatRequest;
import com.amazonaws.services.simpleworkflow.model.ScheduleActivityTaskDecisionAttributes;

/**
 * Context passed to a method annotated with {@link ActivityMethod}
 * <p/>
 * so it can get inputs, set outputs and record heartbeats without having to be configured
 * with Amazon SWF.
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