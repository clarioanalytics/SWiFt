package com.clario.swift;

import org.junit.Assert;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

/**
 * @author George Coller
 */
public class WorkflowBuilderTest {
    String expected = "a1 'Act 1' '1.0'\n"
        + "a2 'Act 1' '1.0'\n"
        + "a3 'Act 2' '2.0'\n"
        + "b1 'Act 1' '1.0' parents(a2, a3)\n"
        + "b2 'Act 2' '2.0' parents(a2, a3)\n"
        + "b3 'Act 3' '1.0' parents(a2, a3)\n"
        + "c 'Act 1' '2.0' parents(b1, b2, b3)\n"
        + "d 'Act 1' '2.0' parents(CHECKPOINT:1)\n"
        + "f 'Act 3' '1.0' parents(CHECKPOINT:1)\n"
        + "g 'Act 1' '2.0' parents(f)\n"
        + "CHECKPOINT:1 parents(a1, c)\n";

    @Test
    public void testWithAddParents() {
        WorkflowBuilder builder = new WorkflowBuilder()
            .activity("a1", "Act 1", "1.0")

            .activity("a2", "Act 1", "1.0")
            .activity("a3", "Act 2", "2.0")

            .activity("b1", "Act 1", "1.0")
            .activity("b2", "Act 2", "2.0")
            .activity("b3", "Act 3", "1.0")
            .withTasks("b.*").addParents("a2|a3").retry(3, TimeUnit.SECONDS.toMillis(5))

            .activity("c", "Act 1", "2.0").addParents("b.*")

            .checkpoint() // TODO: Parents across checkpoints is still sketchy

            .activity("d", "Act 1", "2.0")
            .activity("f", "Act 3", "1.0")
            .activity("g", "Act 1", "2.0").addParents("f");
        Assert.assertEquals(expected, builder.toString());
        builder.buildTaskList();
    }

}
