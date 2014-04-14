package com.clario.swift;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 * @author George Coller
 */
public class DecisionBuilderTest {
    @Test
    public void testBalls() {
        DecisionBuilder b = new DecisionBuilder();

        b.activity("a1", "Act 1", "1.0")
            .activity("a2", "Act 1", "1.0").group("a")
            .activity("a3", "Act 2", "2.0").group("a")
            .activity("b1", "Act 1").groupAndParent("b", "a").retry(3, TimeUnit.SECONDS.toMillis(5))
            .activity("b2", "Act 2").groupAndParent("b", "a")
            .activity("b3", "Act 3", "1.0").group("b").parent("a")
            .activity("c", "Act 1", "2.0").parent("b").mark()
            .activity("d", "Act 1")
            .activity("f", "Act 3")
            .activity("g", "Act 1").parent("f");

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

        Assert.assertEquals(expected, b.toString());
    }
}
