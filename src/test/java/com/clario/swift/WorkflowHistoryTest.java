package com.clario.swift;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static com.clario.swift.MockWorkflowHistory.loadHistoryEventList;
import static org.junit.Assert.assertEquals;

public class WorkflowHistoryTest {
    private WorkflowHistory history;


    @Before
    public void before() {
        history = new WorkflowHistory();
    }

    @Test
    public void testFilterEvents() throws Exception {
        history.addHistoryEvents(loadHistoryEventList(WorkflowHistoryTest.class, "SimpleWorkflowHistory.json"));
        assertEquals(3, history.filterActionEvents("step1").size());
        assertEquals(3, history.filterActionEvents("step2").size());
        assertEquals(3, history.filterActionEvents("step3").size());
    }


    @Test
    public void testGetMarkers() throws Exception {
        history.addHistoryEvents(loadHistoryEventList(WorkflowHistoryTest.class, "RetryWorkflowHistory.json"));
        List<ActionEvent> markers = history.getMarkers();
        assertEquals(1, markers.size());
        assertEquals("failUntilTime", markers.get(0).getActionId());
        assertEquals("1398724533227", markers.get(0).getData1());
    }

    @Test
    public void testGetSignals() throws Exception {
        history.addHistoryEvents(loadHistoryEventList(WorkflowHistoryTest.class, "WaitForSignalWorkflow.json"));
        List<ActionEvent> signals = history.getSignals();
        assertEquals(1, signals.size());
        assertEquals("Boo", signals.get(0).getActionId());
        assertEquals("99", signals.get(0).getData1());
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

}