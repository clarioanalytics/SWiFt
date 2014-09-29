package com.clario.swift;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static com.amazonaws.services.simpleworkflow.model.EventType.ActivityTaskCompleted;
import static com.clario.swift.MockWorkflowHistory.loadHistoryEventList;
import static org.junit.Assert.assertEquals;

public class WorkflowHistoryTest {
    private WorkflowHistory history;


    @Before
    public void before() {
        history = new WorkflowHistory();
    }

    @Test
    public void testFilterActionEvents() throws Exception {
        history.addHistoryEvents(loadHistoryEventList(WorkflowHistoryTest.class, "SimpleWorkflowHistory.json"));
        assertEquals(3, history.filterActionEvents("step1").size());
        assertEquals(3, history.filterActionEvents("step2").size());
        assertEquals(3, history.filterActionEvents("step3").size());
    }

    @Test
    public void testFilterEvents() throws Exception {
        // trim off last decision and workflow completed
        MockWorkflowHistory mock = mockWorkflowHistory("SimpleWorkflowHistory.json").withEventIdInRange(1, 19);
        history.addHistoryEvents(mock.getEvents());
        assertEquals(12, history.filterEvents(null, null, false).size());
        assertEquals(3, history.filterEvents(null, null, true).size());
        assertEquals(3, history.filterEvents("step1", null, false).size());
        assertEquals(3, history.filterEvents(null, ActivityTaskCompleted, false).size());
    }


    @Test
    public void testGetMarkers() throws Exception {
        history.addHistoryEvents(loadHistoryEventList(WorkflowHistoryTest.class, "RetryWorkflowHistory.json"));
        List<ActionEvent> markers = history.getMarkers(false);
        assertEquals(1, markers.size());
        assertEquals("failUntilTime", markers.get(0).getActionId());
        assertEquals("1398724533227", markers.get(0).getData1());
    }

    @Test
    public void testGetSignalsAll() throws Exception {
        history.addHistoryEvents(loadHistoryEventList(WorkflowHistoryTest.class, "WaitForSignalWorkflow.json"));
        List<ActionEvent> signals = history.getSignals(false);
        assertEquals(1, signals.size());
        assertEquals("Boo", signals.get(0).getActionId());
        assertEquals("99", signals.get(0).getData1());
    }

    @Test
    public void testGetSignalsSinceLastDecisionExist() throws Exception {
        MockWorkflowHistory mock = mockWorkflowHistory("WaitForSignalWorkflow.json")
            .withStopAtDecisionTaskStarted(2);
        history.addHistoryEvents(mock.getEvents());
        List<ActionEvent> signals = history.getSignals(true);
        assertEquals(1, signals.size());
        assertEquals("Boo", signals.get(0).getActionId());
        assertEquals("99", signals.get(0).getData1());
    }

    @Test
    public void testGetSignalsSinceLastDecisionNotExist() throws Exception {
        MockWorkflowHistory mock = mockWorkflowHistory("WaitForSignalWorkflow.json")
            .withStopAtDecisionTaskStarted(3);
        history.addHistoryEvents(mock.getEvents());
        List<ActionEvent> signals = history.getSignals(true);
        assertEquals(0, signals.size());
    }

    @Test
    public void testGetWorkflowInput() throws Exception {
        history.addHistoryEvents(loadHistoryEventList(WorkflowHistoryTest.class, "SimpleWorkflowHistory.json"));
        assertEquals("100", history.getWorkflowInput());
    }

    @Test
    public void testGetErrorEvents() throws Exception {
        history.addHistoryEvents(loadHistoryEventList(WorkflowHistoryTest.class, "ScheduleActivityTaskFailed.json"));
        assertEquals(1, history.getErrorEvents().size());
    }

    private MockWorkflowHistory mockWorkflowHistory(String name) {
        MockWorkflowHistory mock = new MockWorkflowHistory();
        mock.loadEvents(WorkflowHistoryTest.class, name);
        return mock;
    }

}