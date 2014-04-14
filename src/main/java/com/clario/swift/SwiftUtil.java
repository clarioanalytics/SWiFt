package com.clario.swift;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

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

    public static boolean isNotEmpty(String s) {
        return !(s == null || s.length() == 0);
    }

    public static <T> T firstOrNull(List<T> list) {
        return list.isEmpty() ? null : list.get(0);
    }

    public static <T> T first(List<T> list) {
        if (list.isEmpty()) {
            throw new NoSuchElementException("first not available on empty list");
        }
        return list.get(0);
    }

    public static <T> List<T> tail(List<T> list) {
        if (list.isEmpty()) {
            throw new NoSuchElementException("tail not available on empty list");
        }
        return list.subList(1, list.size());
    }

    public static <T> T defaultIfNull(T value, T replacement) {
        return value == null ? replacement : value;
    }

    public static String join(List<String> items, String separator) {
        separator = defaultIfNull(separator, "");
        int size = items.size();
        StringBuilder b = new StringBuilder((10 + separator.length()) * items.size());
        for (int i = 0; i < size; i++) {
            b.append(items.get(i));
            if (i < size - 1) {
                b.append(separator);
            }
        }
        return b.toString();
    }
}
