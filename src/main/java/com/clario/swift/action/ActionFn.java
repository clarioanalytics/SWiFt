package com.clario.swift.action;

/**
 * Function that returns an {@link Action} 
 * @author George Coller
 */
public interface ActionFn<T extends Action> {
    T apply();
}
