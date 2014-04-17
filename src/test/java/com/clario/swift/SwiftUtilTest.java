package com.clario.swift;

import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.clario.swift.SwiftUtil.*;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static org.junit.Assert.*;

/**
 * @author George Coller
 */
public class SwiftUtilTest {

    @Test
    public void testDefaultIfNull() {
        assertEquals("Bob", defaultIfNull("Bob", "Jones"));
        assertEquals("", defaultIfNull("", "Jones"));
        assertEquals("Jones", defaultIfNull(null, "Jones"));
    }

    @Test
    public void testIsNotEmpty() {
        assertTrue(isNotEmpty(" "));
        assertTrue(isNotEmpty("ABC"));
        assertFalse(isNotEmpty(""));
        assertFalse(isNotEmpty(null));
    }

    @Test
    public void testJson() {
        String expected = "{\"A\":1,\"B\":{\"C\":[1,2,3]},\"D\":null}";
        assertEquals(expected, toJson(fromJson(expected)));
    }

    @Test
    public void testJoin() {
        assertEquals("", join(emptySet(), null));
        assertEquals("", join(emptySet(), ""));
        assertEquals("", join(emptySet(), ",,,"));
        assertEquals("ABC", join(asList("A", "B", "C"), null));
        assertEquals("ABC", join(asList("A", "B", "C"), ""));
        assertEquals("A,B,C", join(asList("A", "B", "C"), ","));
        assertEquals("A||B||C", join(asList("A", "B", "C"), "||"));
    }

    @Test
    public void testJoinEntries() {
        List<String> empty = emptyList();
        assertEquals(empty, joinEntries(emptyMap(), null));
        assertEquals(empty, joinEntries(emptyMap(), ""));
        assertEquals(empty, joinEntries(emptyMap(), ",,,"));
        Map<String, Integer> map = new HashMap<>();
        map.put("A", 1);
        map.put("B", 2);
        map.put("C", null);
        assertEquals(Arrays.asList("A1", "B2", "C"), joinEntries(map, null));
        assertEquals(Arrays.asList("A1", "B2", "C"), joinEntries(map, ""));
        assertEquals(Arrays.asList("A-1", "B-2", "C-"), joinEntries(map, "-"));
        assertEquals(Arrays.asList("A||1", "B||2", "C||"), joinEntries(map, "||"));
    }
}
