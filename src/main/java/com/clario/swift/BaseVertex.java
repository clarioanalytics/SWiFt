package com.clario.swift;

import java.util.Set;
import java.util.TreeSet;

/**
 * @author George Coller
 */
public class BaseVertex<B> implements Vertex<BaseVertex<B>> {

    private final String id;
    private final Set<BaseVertex<B>> parents = new TreeSet<>();

    public BaseVertex(String id) {
        this.id = id;
    }

    @Override
    public String getId() {
        return null;
    }

    @Override
    public Set<BaseVertex<B>> getParents() {
        return null;
    }

}
