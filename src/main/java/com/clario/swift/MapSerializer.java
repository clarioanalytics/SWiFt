package com.clario.swift;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Serialize to/from a Map&lt;String,String&gt;.
 *
 * @author George Coller
 */
public class MapSerializer {
    public Map<String, String> unmarshal(String value) {
        Map<String, String> map = new LinkedHashMap<>();
        Map<String, Object> unmarshal = SwiftUtil.fromJson(value);
        for (Map.Entry<String, Object> entry : unmarshal.entrySet()) {
            map.put(entry.getKey(), entry.getValue() == null ? null : entry.getValue().toString());
        }
        return map;
    }

    public String marshal(Map<String, String> map) {
        ObjectNode obj = JsonNodeFactory.instance.objectNode();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            obj.put(entry.getKey(), entry.getValue());
        }
        return SwiftUtil.toJson(obj);
    }
}
