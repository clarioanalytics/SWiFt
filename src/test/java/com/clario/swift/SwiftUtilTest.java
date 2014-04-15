package com.clario.swift;

import org.junit.Assert;
import org.junit.Test;

import java.util.*;

import static com.clario.swift.SwiftUtil.cycleCheck;

/**
 * @author George Coller
 */
public class SwiftUtilTest {

    @Test
    public void testCycleCheckValid() {
        List<MockV> list = makeValidGraph();
        cycleCheck(list);
    }

    @Test
    public void testCycleEndToBeginning() {
        List<MockV> list = makeValidGraph();
        list.get(7).addAll(list.get(0));
        try {
            cycleCheck(list);
        } catch (IllegalStateException e) {
            Assert.assertEquals("Cycle detected: 0 -> 2 -> 5 -> 7 -> 0", e.getMessage());
            return;
        }
        Assert.fail();
    }

    @Test
    public void testCycleInner() {
        List<MockV> list = makeValidGraph();
        list.get(4).addAll(list.get(3));
        list.get(3).addAll(list.get(4));
        try {
            cycleCheck(list);
        } catch (IllegalStateException e) {
            Assert.assertEquals("Cycle detected: 0 -> 2 -> 3 -> 4 -> 3", e.getMessage());
            return;
        }
        Assert.fail();
    }

    @Test
    public void testCycleSelfReference() {
        List<MockV> list = makeValidGraph();
        list.get(4).addAll(list.get(4));
        try {
            cycleCheck(list);
        } catch (IllegalStateException e) {
            Assert.assertEquals("Cycle detected: 0 -> 2 -> 4 -> 4", e.getMessage());
            return;
        }
        Assert.fail();
    }

    public List<MockV> makeValidGraph() {
        MockV[] vs = new MockV[8];
        for (int i = 0; i < vs.length; i++) {
            vs[i] = new MockV(String.valueOf(i));
        }
        vs[0].addAll(vs[1], vs[2], vs[6]);
        vs[1].addAll(vs[3], vs[5]);
        vs[2].addAll(vs[3], vs[4], vs[5]);
        vs[4].addAll(vs[6]);
        vs[5].addAll(vs[7]);
        List<MockV> list = new ArrayList<>();
        Collections.addAll(list, vs);
        return list;
    }


    private static class MockV implements Vertex {
        private final String stepId;
        private final Set<MockV> children = new HashSet<>();

        private MockV(String stepId) {
            this.stepId = stepId;
        }

        void addAll(MockV... kids) {
            Collections.addAll(children, kids);
        }

        @Override
        public String getStepId() { return stepId; }

        @Override
        public Set<? extends Vertex> getChildren() { return children; }

        @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
        public boolean equals(Object o) { return stepId.equals(((MockV) o).stepId); }

        public int hashCode() { return stepId.hashCode(); }

        public String toString() {
            return stepId;
        }
    }
}
