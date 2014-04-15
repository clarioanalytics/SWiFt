package com.clario.swift;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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


    public static void cycleCheck(List<? extends Vertex> vertices) {
        List<Vertex> checked = new ArrayList<>(vertices.size());
        for (Vertex vertex : vertices) {
            walk(checked, new ArrayList<Vertex>(), vertex);
        }
    }

    private static void walk(List<Vertex> checked, List<Vertex> route, Vertex vertex) {
        if (route.contains(vertex)) {
            route.add(vertex);
            throw new IllegalStateException("Cycle detected: " + join(route, " -> "));
        }
        route.add(vertex);
        if (vertex.getChildren().isEmpty()) {
            checked.add(vertex);
        } else {
            for (Vertex child : vertex.getChildren()) {
                if (!checked.contains(child)) {
                    walk(checked, new ArrayList<>(route), child);
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

    public static <T> String join(List<T> items, String separator) {
        separator = defaultIfNull(separator, "");
        int size = items.size();
        StringBuilder b = new StringBuilder((10 + separator.length()) * items.size());
        for (int i = 0; i < size; i++) {
            b.append(valueOf(items.get(i)));
            if (i < size - 1) {
                b.append(separator);
            }
        }
        return b.toString();
    }
}
