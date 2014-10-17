package com.clario.swift;

import com.clario.swift.event.Event;
import com.clario.swift.event.EventState;
import org.junit.Test;

import static com.amazonaws.services.simpleworkflow.model.EventType.*;
import static com.clario.swift.EventList.*;
import static com.clario.swift.TestUtil.loadActionEvents;
import static org.junit.Assert.assertEquals;

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
            .select(byEventState(EventState.ACTIVE));
        assertEquals(6, events.size());
        for (Event event : events) {
            assertEquals(EventState.ACTIVE, event.getState());
        }
    }

    @Test
    public void testGetMarkers() {
        EventList markers = loadEventList("RetryWorkflowHistory.json").selectEventType(MarkerRecorded);
        assertEquals(1, markers.size());
        Event event = markers.getFirst();
        assertEquals("failUntilTime", event.getActionId());
        assertEquals("1398724533227", event.getOutput());
    }

    @Test
    public void testGetSignalsAll() {
        EventList signals = loadEventList("WaitForSignalWorkflow.json").selectEventType(WorkflowExecutionSignaled);
        assertEquals(1, signals.size());
        Event event = signals.getFirst();
        assertEquals("Boo", event.getActionId());
        assertEquals("99", event.getOutput());
    }

    @Test
    public void testGetSignalsSinceLastDecisionExist() {
        EventList signals = loadEventList("WaitForSignalWorkflow.json")
            .select(TestUtil.byUpToDecision(2))
            .select(bySinceLastDecision(),
                byEventType(WorkflowExecutionSignaled));

        assertEquals(1, signals.size());
        Event event = signals.getFirst();
        assertEquals("Boo", event.getActionId());
        assertEquals("99", event.getOutput());
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
            .select(byEventState(EventState.CRITICAL));
        assertEquals(1, events.size());
    }

    private EventList loadEventList(String fileName) {
        return loadActionEvents(EventListTest.class, fileName);
    }

}