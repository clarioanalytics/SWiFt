package com.clario.swift;

import java.util.Map;

/**
 * @author George Coller
 */
public interface ActivityContext {

    String getId();

    void recordHeartbeat(String details);

    Map<String, String> getInputs();

    void setOutput(String value);
}