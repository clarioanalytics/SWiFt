package com.clario.swift;

import java.util.*;

import static com.clario.swift.SwiftUtil.join;

/**
 * Graph/Tree functionality as a base class.
 * <p/>
 * Extract the graph functionality of SWF tasks for easier development and unit testing.
 *
 * @author George Coller
 */
public class Vertex<B extends Vertex> implements Comparable<B> {

    protected final String id;
    protected final Set<B> parents = new TreeSet<>();

    public Vertex(String id) {
        this.id = id;
    }

    public String getId() {
        return id;
    }

    public Set<B> getParents() {
        return parents;
    }

    @SafeVarargs
    public final void addParents(B... parents) {
        Collections.addAll(getParents(), parents);
    }

    /**
     * Find a parent by <code>id</code>.
     *
     * @throws IllegalArgumentException if parent not found
     */
    public B getParent(String id) {
        for (B parent : parents) {
            if (parent.getId().equals(id)) {
                return parent;
            }
        }
        throw new IllegalArgumentException("Parent not found: " + id);
    }

    /**
     * Find vertices with no children.
     */
    public static <V extends Vertex<V>> Map<String, V> findLeaves(Map<String, V> vertices) {
        Map<String, V> leaves = new LinkedHashMap<>(vertices);
        for (V vertex : vertices.values()) {
            for (V parent : vertex.getParents()) {
                leaves.remove(parent.getId());
            }
        }
        return leaves;
    }

    /**
     * Scan a graph of vertices and return all detected cycles.
     * <p/>
     * A cycle is a route through the graph that loops:
     * <pre>
     *     V1 -> V2 -> V3 -> V1
     *     V1 -> V1
     * </pre>
     *
     * @param vertices vertices
     * @param <V> extends Vertex
     *
     * @return list of zero or more detected cycles in the graph.
     */
    public static <V extends Vertex<V>> List<List<V>> detectCycles(Collection<V> vertices) {
        List<V> checked = new ArrayList<>();
        List<List<V>> cycles = new ArrayList<>();
        for (V vertex : vertices) {
            detectCyclesWalker(vertex, checked, new ArrayList<V>(), cycles);
        }
        return cycles;
    }

    private static <V extends Vertex<V>> void detectCyclesWalker(V vertex, List<V> checked, List<V> route, List<List<V>> cycles) {
        if (route.contains(vertex)) {
            route.add(vertex);
            Collections.reverse(route);
            cycles.add(route);
            return;
        }
        route.add(vertex);
        if (vertex.getParents().isEmpty()) {
            checked.add(vertex);
        } else {
            for (V parent : vertex.getParents()) {
                if (!checked.contains(parent)) {
                    detectCyclesWalker(parent, checked, new ArrayList<>(route), cycles);
                }
            }
            checked.add(vertex);
        }
    }

    /**
     * Call {@link #detectCycles(java.util.Collection)} and throw exception if one or more cycles exist.
     *
     * @param vertices graph to check
     * @param <V> extends Vertex
     *
     * @throws AssertionError if one or more cycles exist
     */
    public static <V extends Vertex<V>> void assertNoCycles(Collection<V> vertices) {
        List<List<V>> cycles = detectCycles(vertices);
        if (!cycles.isEmpty()) {
            List<String> routes = new ArrayList<>(cycles.size());
            for (List<V> cycle : cycles) {
                routes.add(join(collectIds(cycle), " -> "));
            }
            throw new AssertionError("Cycles detected: " + (routes.size() > 1 ? '\n' : "") + join(routes, "\n"));
        }
    }

    /**
     * Utility method to convert a collection of vertices into a list of vertex ids.
     */
    public static <V extends Vertex<V>> List<String> collectIds(Collection<V> vertices) {
        List<String> ids = new ArrayList<>(vertices.size());
        for (V vertex : vertices) {
            ids.add(vertex.getId());
        }
        return ids;
    }

    /**
     * Utility method to convert a collection of vertices into a map of vertex ids, vertex entries.
     *
     * @return {@link LinkedHashMap} to preserve incoming collection order.
     */
    public static <V extends Vertex<V>> Map<String, V> collectEntries(Collection<V> vertices) {
        Map<String, V> map = new LinkedHashMap<>(vertices.size());
        for (V vertex : vertices) {
            map.put(vertex.getId(), vertex);
        }
        return map;
    }

    @Override
    public boolean equals(Object o) {
        return this == o || o instanceof Task && id.equals(((Task) o).id);
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + ": " + id;
    }

    @Override
    public int compareTo(B b) {
        return id.compareTo(b.getId());
    }
}
