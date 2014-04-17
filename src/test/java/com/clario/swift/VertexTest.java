package com.clario.swift;

import org.junit.Assert;
import org.junit.Test;

import java.util.*;

import static com.clario.swift.SwiftUtil.join;
import static com.clario.swift.Vertex.assertNoCycles;
import static com.clario.swift.Vertex.findLeaves;
import static org.junit.Assert.assertEquals;

/**
 * @author George Coller
 */
public class VertexTest {
    @Test
    public void testFindLeafNodes() {
        Map<String, MockBV> map = new LinkedHashMap<>();
        for (MockBV v : makeValidGraph()) {
            map.put(v.getId(), v);
        }
        Map<String, MockBV> actual = findLeaves(map);
        assertEquals("3,6,7", join(actual.keySet(), ","));
    }

    @Test
    public void testCycleCheckValid() {
        List<MockBV> list = makeValidGraph();
        Vertex.assertNoCycles(list);
    }

    @Test
    public void testCycleEndToBeginning() {
        List<MockBV> list = makeValidGraph();
        list.get(0).addParents(list.get(7));
        try {
            assertNoCycles(list);
        } catch (AssertionError e) {
            String expected = "Cycles detected: \n" +
                "0 -> 1 -> 5 -> 7 -> 0\n" +
                "0 -> 2 -> 5 -> 7 -> 0";
            assertEquals(expected, e.getMessage());
            return;
        }
        Assert.fail();
    }

    @Test
    public void testCycleInner() {
        List<MockBV> list = makeValidGraph();
        list.get(3).addParents(list.get(4));
        list.get(4).addParents(list.get(3));
        try {
            assertNoCycles(list);
        } catch (AssertionError e) {
            assertEquals("Cycles detected: 3 -> 4 -> 3", e.getMessage());
            return;
        }
        Assert.fail();
    }

    @Test
    public void testCycleSelfReference() {
        List<MockBV> list = makeValidGraph();
        list.get(4).addParents(list.get(4));
        try {
            assertNoCycles(list);
        } catch (AssertionError e) {
            assertEquals("Cycles detected: 4 -> 4", e.getMessage());
            return;
        }
        Assert.fail();
    }

    private List<MockBV> makeValidGraph() {
        MockBV[] bvs = new MockBV[8];
        for (int i = 0; i < bvs.length; i++) {
            bvs[i] = new MockBV(String.valueOf(i));
        }
        bvs[1].addParents(bvs[0]);
        bvs[2].addParents(bvs[0]);
        bvs[3].addParents(bvs[1], bvs[2]);
        bvs[4].addParents(bvs[2]);
        bvs[5].addParents(bvs[1], bvs[2]);
        bvs[6].addParents(bvs[0], bvs[4]);
        bvs[7].addParents(bvs[5]);
        List<MockBV> list = new ArrayList<>();
        Collections.addAll(list, bvs);
        return list;
    }

    static class MockBV extends Vertex<MockBV> {
        public MockBV(String id) {
            super(id);
        }
    }
}
