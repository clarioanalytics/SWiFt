package com.clario.swift.action;

import com.clario.swift.DecisionBuilder;

/**
 * Callback used with {@link DecisionBuilder} for delayed setting of inputs.
 * Function that returns an {@link Action}
 *
 * @author George Coller
 */
public interface ActionCallback<T extends Action> {
    /**
     * Apply the function.
     *
     * @return the action that was just applied to this function.
     */
    T apply();
}
