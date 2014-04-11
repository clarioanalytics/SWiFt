package com.clario.swift;

import java.util.Map;

/**
 * @author George Coller
 */
public interface ActivityContext {

    String getStepId();

    void recordHeartbeat(String details);

    Map<String, String> getInputs();

    Map<String, String> getOutputs();

    void setOutput(String value);
}