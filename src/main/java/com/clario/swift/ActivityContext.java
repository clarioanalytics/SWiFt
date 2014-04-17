package com.clario.swift;

import java.util.Map;

/**
 * Context passed to an {@link ActivityMethod} so it can get inputs, set outputs and record heartbeats without having to be configured
 * with Amazon SWF.
 *
 * @author George Coller
 */
public interface ActivityContext {

    String getId();

    void recordHeartbeat(String details);

    Map<String, String> getInputs();

    void setOutput(String value);
}