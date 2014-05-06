package com.clario.swift;

import org.junit.Assert;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.TimeUnit;

import static com.clario.swift.SwiftUtil.*;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.concurrent.TimeUnit.DAYS;
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
        assertEquals((Integer) 2, defaultIfNull(null, 2));
        assertEquals((Integer) 3, defaultIfNull(3, 2));
    }

    @Test
    public void testDefaultIfEmpty() {
        assertEquals("Bob", defaultIfEmpty("Bob", "Jones"));
        assertEquals("Jones", defaultIfEmpty("", "Jones"));
        assertEquals("Jones", defaultIfEmpty(null, "Jones"));
        assertEquals("2", defaultIfEmpty(null, 2));
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

    @Test
    public void testTrimToMaxLength() {
        assertEquals(null, trimToMaxLength(null, 0));
        assertEquals(null, trimToMaxLength(null, 900));
        assertEquals("", trimToMaxLength("", 0));
        assertEquals("", trimToMaxLength("", 900));
        assertEquals("", trimToMaxLength("abc", 0));
        assertEquals("a", trimToMaxLength("abc", 1));
        assertEquals("ab", trimToMaxLength("abc", 2));
        assertEquals("abc", trimToMaxLength("abc", 3));
        assertEquals("abc", trimToMaxLength("abc", 4));
    }

    @Test
    public void testAssertMaxLength() {
        assertEquals(null, assertMaxLength(null, 0));
        assertEquals(null, assertMaxLength(null, 900));
        assertEquals("", assertMaxLength("", 0));
        assertEquals("", assertMaxLength("", 900));
        assertBadMaxLength("abc", 0);
        assertBadMaxLength("abc", 1);
        assertBadMaxLength("abc", 2);
        assertEquals("abc", assertMaxLength("abc", 3));
        assertEquals("abc", assertMaxLength("abc", 4));
    }

    private static void assertBadMaxLength(String value, int length) {
        try {
            assertMaxLength(value, length);
        } catch (AssertionError e) {
            return;
        }
        fail(String.format("Expected '%s', %d to fail", value, length));
    }

    @Test
    public void testAssertSWFValue() {
        assertSwfValue("1");
        assertSwfValue("a");
        assertBadSwfValue("");
        assertBadSwfValue(" a");
        assertBadSwfValue("a ");
        assertBadSwfValue("a\t");
        assertBadSwfValue("\ta");

        List<String> badStrings = new ArrayList<>();
        badStrings.addAll(Arrays.asList(":", "/", "|", "arn"));
        for (char i = 0x0000; i < 0x001f; i++) { badStrings.add(String.valueOf(i)); }
        for (char i = 0x007f; i < 0x009f; i++) { badStrings.add(String.valueOf(i)); }

        for (String s : badStrings) {
            assertBadSwfValue(s);
            assertBadSwfValue("a" + s);
            assertBadSwfValue(s + "a");
            assertBadSwfValue("a" + s + "a");
        }
    }

    private static void assertBadSwfValue(String value) {
        try {
            assertSwfValue(value);
        } catch (AssertionError e) {
            return;
        }
        fail(String.format("Expected '%s' to fail", value));
    }

    @Test
    public void testCalcTimeoutString() {
        assertEquals(TIMEOUT_NONE, calcTimeoutString(null, -1));
        assertEquals(TIMEOUT_NONE, calcTimeoutString(null, 0));
        assertEquals(TIMEOUT_NONE, calcTimeoutString(null, 1));
        assertEquals(TIMEOUT_NONE, calcTimeoutString(DAYS, -1));
        assertEquals(TIMEOUT_NONE, calcTimeoutString(DAYS, 0));
        assertEquals(String.valueOf(1), calcTimeoutString(TimeUnit.SECONDS, 1));
        assertEquals(String.valueOf(60), calcTimeoutString(TimeUnit.MINUTES, 1));
        assertEquals(String.valueOf(60 * 60), calcTimeoutString(TimeUnit.HOURS, 1));
    }

    @Test
    public void testCalcWorkflowId() throws Exception {
        assertWorkflowId("", "");
        assertWorkflowId("A B C", "A_B_C");
        assertWorkflowId("A  B  C", "A__B__C");
        assertWorkflowId(" A  B  C ", "A__B__C");
        String name = "";
        for (char i = 0; i < MAX_ID_LENGTH + 10; i++) {
            name += i % 10;
            assertWorkflowId(name, trimToMaxLength(name, MAX_ID_LENGTH - 25));
        }
    }

    @Test
    public void testReadFile() {
        assertEquals("A\nB\nC\nD", readFile(SwiftUtil.class, "SwiftUtilTestFile.txt"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testReadMissingFile() {
        assertEquals("A\nB\nC\nD", readFile(SwiftUtil.class, "NotThere.txt"));
    }

    private void assertWorkflowId(String name, String expected) {
        String regEx = "\\.\\d{4}-\\d{2}-\\d{2}T\\d{2}\\.\\d{2}\\.\\d{2}\\.\\d{3}Z";
        String uniqueWorkflowId = createUniqueWorkflowId(name);
        Assert.assertTrue(uniqueWorkflowId.matches(".*" + regEx));
        String[] split = uniqueWorkflowId.split(regEx);
        String actual = split.length == 0 ? "" : split[0];
        Assert.assertEquals(expected, actual);
    }
}
