package com.clario.swift;

import java.util.Set;

/**
 * Simple Directed Graph Vertex
 *
 * @author George Coller
 */
interface Vertex<T extends Vertex> {
    String getId();

    Set<T> getParents();
}
