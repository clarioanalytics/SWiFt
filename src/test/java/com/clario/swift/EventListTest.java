package com.clario.swift;

import org.junit.Test;

import java.util.Map;

import static com.amazonaws.services.simpleworkflow.model.EventType.*;
import static com.clario.swift.Event.Field;
import static com.clario.swift.EventList.*;
import static com.clario.swift.TestUtil.loadActionEvents;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class EventListTest {

    private static void assertEventIdSequence(EventList list, long startEventId) {
        for (Event event : list) {
            assertEquals(startEventId, event.getEventId().longValue());
            startEventId--;
        }
    }

    @Test
    public void testByActionId() {
        EventList selected = loadEventList("SimpleWorkflowHistory.json")
            .select(byActionId("step2"));
        assertEquals(3, selected.size());
        assertEventIdSequence(selected, 13);
    }

    @Test
    public void testByAfterPriorDecision() {
        EventList selected = loadEventList("SimpleWorkflowHistory.json")
            .select(byEventIdRange(1L, 21L), bySinceLastDecision());
        assertEquals(5, selected.size());
        assertEventIdSequence(selected, 21);
    }

    @Test
    public void testByEventType() {
        EventList mock = loadEventList("SimpleWorkflowHistory.json").select(byEventType(ActivityTaskCompleted));
        assertEquals(3, mock.size());
        for (Event historyEvent : mock) {
            assertEquals(ActivityTaskCompleted, historyEvent.getType());
        }
    }

    @Test
    public void testByActionState() {
        EventList events = loadEventList("SimpleWorkflowHistory.json")
            .select(byEventState(Event.State.ACTIVE));
        assertEquals(6, events.size());
        for (Event historyEvent : events) {
            assertEquals(Event.State.ACTIVE, historyEvent.getActionState());
        }
    }

    @Test
    public void testGetMarkers() {
        EventList markers = loadEventList("RetryWorkflowHistory.json")
            .select(byEventType(MarkerRecorded));
        assertEquals(1, markers.size());
        assertEquals("failUntilTime", markers.get(0).getActionId());
        assertEquals("1398724533227", markers.get(0).getData1());
    }

    @Test
    public void testGetSignalsAll() {
        EventList signals = loadEventList("WaitForSignalWorkflow.json")
            .select(byEventType(WorkflowExecutionSignaled));
        assertEquals(1, signals.size());
        assertEquals("Boo", signals.get(0).getActionId());
        assertEquals("99", signals.get(0).getData1());
    }

    @Test
    public void testGetSignalsSinceLastDecisionExist() {
        EventList signals = loadEventList("WaitForSignalWorkflow.json")
            .select(TestUtil.byUpToDecision(2))
            .select(bySinceLastDecision(),
                byEventType(WorkflowExecutionSignaled));

        assertEquals(1, signals.size());
        assertEquals("Boo", signals.get(0).getActionId());
        assertEquals("99", signals.get(0).getData1());
    }

    @Test
    public void testGetSignalsSinceLastDecisionNotExist() {
        EventList signals = loadEventList("WaitForSignalWorkflow.json")
            .select(TestUtil.byUpToDecision(3))
            .select(bySinceLastDecision(),
                byEventType(WorkflowExecutionSignaled));
        assertEquals(0, signals.size());
    }

    @Test
    public void testGetErrorEvents() {
        EventList events = loadEventList("ScheduleActivityTaskFailed.json")
            .select(byEventState(Event.State.CRITICAL));
        assertEquals(1, events.size());
    }

    @Test
    public void testCreateMap() {
        Map<Field, Object> map = createFieldMap(Field.dataField1, "data1",
            Field.dataField2, null,
            Field.eventId, 99L);
        assertEquals(3, map.size());
        assertEquals("data1", map.get(Field.dataField1));
        assertNull(map.get(Field.dataField2));
        assertEquals(99L, map.get(Field.eventId));
    }

    private EventList loadEventList(String fileName) {
        return loadActionEvents(EventListTest.class, fileName);
    }

}