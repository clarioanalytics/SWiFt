package com.clario.swift;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;

/**
 * Utility methods.
 *
 * @author George Coller
 */
public class SwiftUtil {
    public static final ObjectMapper JSON_OBJECT_MAPPER = new ObjectMapper();

    /**
     * Convert an object into a JSON string.
     *
     * @param o object to convert
     *
     * @return JSON string.
     * @see com.fasterxml.jackson.databind.ObjectMapper for details on what can be converted.
     */
    public static String toJson(Object o) {
        try {
            return JSON_OBJECT_MAPPER.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize to JSON", e);
        }
    }

    /**
     * Convert a JSON string into a structure of Java collections.
     *
     * @param json string to convert
     *
     * @return converted structure
     * @see com.fasterxml.jackson.databind.ObjectMapper for details on what can be converted.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> fromJson(String json) {
        try {
            return JSON_OBJECT_MAPPER.readValue(json, Map.class);
        } catch (IOException e) {
            throw new IllegalStateException(format("Failed to unmarshal JSON: \"%s\"", json), e);
        }
    }

    /**
     * @return true if the parameter is not null or has a length greater than zero
     */
    public static boolean isNotEmpty(String s) {
        return !(s == null || s.length() == 0);
    }

    /**
     * @return replacement parameter if the value parameter is null, otherwise return value parameter.
     */
    public static <T> T defaultIfNull(T value, T replacement) {
        return value == null ? replacement : value;
    }

    /**
     * Convert a collection of items into a string with each item separated by the separator parameter.
     *
     * @param items collection to join
     * @param separator string to insert between each item, defaults to empty string.
     *
     * @return joined string
     */
    public static <T> String join(Collection<T> items, String separator) {
        separator = defaultIfNull(separator, "");
        int size = items.size();
        StringBuilder b = new StringBuilder((10 + separator.length()) * items.size());
        int i = 0;
        for (T item : items) {
            b.append(item);
            i++;
            if (i < size) {
                b.append(separator);
            }
        }
        return b.toString();
    }

    /**
     * Join each entry of a map into a string using a given separator and returning the resulting list of strings.
     *
     * @param map map to join
     * @param separator string to insert between each key,value pair, defaults to empty string.
     *
     * @return list of joined entries
     */
    public static <A, B> List<String> joinEntries(Map<A, B> map, String separator) {
        List<String> list = new ArrayList<>(map.size());
        separator = defaultIfNull(separator, "");
        for (Map.Entry<A, B> entry : map.entrySet()) {
            list.add(format("%s%s%s", defaultIfNull(entry.getKey(), ""), separator, defaultIfNull(entry.getValue(), "")));
        }
        return list;
    }
}
