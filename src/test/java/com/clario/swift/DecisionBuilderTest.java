package com.clario.swift;

import org.junit.Assert;
import org.junit.Test;

import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author George Coller
 */
public class DecisionBuilderTest {
    String expected = "a1 'Act 1' '1.0'\n"
        + "a2 'Act 1' '1.0' children(b1, b2, b3)\n"
        + "a3 'Act 2' '2.0' children(b1, b2, b3)\n"
        + "b1 'Act 1' '1.0' children(c) parents(a2, a3)\n"
        + "b2 'Act 2' '2.0' children(c) parents(a2, a3)\n"
        + "b3 'Act 3' '1.0' children(c) parents(a2, a3)\n"
        + "c 'Act 1' '2.0' parents(b1, b2, b3)\n"
        + "Decision Group 1\n"
        + "d 'Act 1' '2.0'\n"
        + "f 'Act 3' '1.0' children(g)\n"
        + "g 'Act 1' '2.0' parents(f)\n";

    @Test
    public void testWithAddParents() {
        DecisionBuilder builder = new DecisionBuilder()
            .activity("a1", "Act 1", "1.0")

            .activity("a2", "Act 1", "1.0")
            .activity("a3", "Act 2", "2.0")

            .activity("b1", "Act 1")
            .activity("b2", "Act 2")
            .activity("b3", "Act 3", "1.0")
            .withEach("b.*").addParents("a2|a3").retry(3, TimeUnit.SECONDS.toMillis(5))

            .activity("c", "Act 1", "2.0").addParents("b.*")

            .mark()

            .activity("d", "Act 1")
            .activity("f", "Act 3")
            .activity("g", "Act 1").addParents("f");
        Assert.assertEquals(expected, builder.toString());
        builder.buildStepList();
    }

}
