package com.clario.swift;

/**
 * Context passed to an {@link ActivityMethod} so it can get inputs, set outputs and record heartbeats without having to be configured
 * with Amazon SWF.
 *
 * @author George Coller
 */
public interface ActivityContext {

    String getId();

    void recordHeartbeat(String details);

    String getInput();

    void setOutput(String value);
}