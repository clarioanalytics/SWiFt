package com.clario.swift;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.*;

import static java.lang.String.valueOf;

/**
 * Utility methods.
 *
 * @author George Coller
 */
public class SwiftUtil {
    public static final ObjectMapper JSON_OBJECT_MAPPER = new ObjectMapper();

    public static String toJson(Object o) {
        try {
            return JSON_OBJECT_MAPPER.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize to JSON", e);
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> fromJson(String json) {
        try {
            return JSON_OBJECT_MAPPER.readValue(json, Map.class);
        } catch (IOException e) {
            throw new IllegalStateException(String.format("Failed to unmarshal JSON: \"%s\"", json), e);
        }
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

    public static <V extends Vertex<V>> void cycleCheck(Collection<V> vertices) {
        List<V> checked = new ArrayList<>(vertices.size());
        for (V vertex : vertices) {
            walkCycleCheckRoute(checked, new ArrayList<V>(), vertex);
        }
    }

    private static <V extends Vertex<V>> void walkCycleCheckRoute(List<V> checked, List<V> route, V vertex) {
        if (route.contains(vertex)) {
            route.add(vertex);
            Collections.reverse(route);
            throw new IllegalStateException("Cycle detected: " + join(route, " -> "));
        }
        route.add(vertex);
        if (vertex.getParents().isEmpty()) {
            checked.add(vertex);
        } else {
            for (V parent : vertex.getParents()) {
                if (!checked.contains(parent)) {
                    walkCycleCheckRoute(checked, new ArrayList<>(route), parent);
                }
            }
            checked.add(vertex);
        }
    }

    public static boolean isNotEmpty(String s) {
        return !(s == null || s.length() == 0);
    }

    public static <T> T firstOrNull(List<T> list) {
        return list.isEmpty() ? null : list.get(0);
    }

    public static <T> T defaultIfNull(T value, T replacement) {
        return value == null ? replacement : value;
    }

    public static <T> String join(Collection<T> items, String separator) {
        separator = defaultIfNull(separator, "");
        int size = items.size();
        StringBuilder b = new StringBuilder((10 + separator.length()) * items.size());
        int i = 0;
        for (T item : items) {
            b.append(valueOf(item));
            i++;
            if (i < size) {
                b.append(separator);
            }
        }
        return b.toString();
    }
}
