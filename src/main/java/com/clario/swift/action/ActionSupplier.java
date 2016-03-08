package com.clario.swift.action;

import com.clario.swift.DecisionBuilder;

/**
 * Callback used with {@link DecisionBuilder} for delayed setting of inputs.
 * Function that returns an {@link Action}
 *
 * @author George Coller
 */
public interface ActionSupplier {
    /**
     * @return the action that was just applied with this function.
     */
    Action get();
}
