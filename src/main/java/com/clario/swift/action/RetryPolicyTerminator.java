package com.clario.swift.action;

import com.clario.swift.event.Event;

/**
 * Register with a {@link RetryPolicy} to facilitate a custom method for stopping retries based an Action's output or error values during a decision.
 * <p/>
 * If registered with an on-success retry policy it will be called once with
 * <ul>
 * <li>value = An action's {@link Action#getOutput()} value</li>
 * </ul>
 * If registered with an on-error retry policy it will be called twice with
 * <ul>
 * <li>error reason = The {@link Event#getReason()} value from an error event </li>
 * <li>error details = The {@link Event#getDetails()} value from an error event </li>
 * </ul>
 *
 * @author George Coller
 */
public interface RetryPolicyTerminator {

    /**
     * Called during a decision as a way to sh
     *
     * @param value Action value to test.
     *
     * @return true, if retries should be terminated, otherwise false
     */
    boolean shouldStopRetrying(String value);

}
