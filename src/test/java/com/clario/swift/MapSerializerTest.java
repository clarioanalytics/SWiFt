package com.clario.swift;

import org.junit.Test;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * @author George Coller
 */
public class MapSerializerTest {
    @Test
    public void testUnmarshal() {
        Map<String, String> map = new MapSerializer().unmarshal(serializedString);
        assert map.size() == 7;
        assert map.get("").equals("0");
        assert map.get("a").equals("1");
        assert map.get("b").equals("");
        assert map.get("c").equals("2");
        assert map.get("d") == null;
        assert map.get("e").equals("3");
        assert map.get("f|f").equals("4|4");
    }

    @Test
    public void testMarshal() {
        LinkedHashMap<String, String> map = new LinkedHashMap<>(7);
        map.put("", "0");
        map.put("a", "1");
        map.put("b", "");
        map.put("c", "2");
        map.put("d", null);
        map.put("e", "3");
        map.put("f|f", "4|4");
        String value = new MapSerializer().marshal(map);
        assert value.equals(serializedString);
    }

    private String serializedString = "{\"\":\"0\",\"a\":\"1\",\"b\":\"\",\"c\":\"2\",\"d\":null,\"e\":\"3\",\"f|f\":\"4|4\"}";
}
