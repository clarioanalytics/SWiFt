package com.clario.swift;

import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @author George Coller
 */
public class WorkflowTest {
    Workflow workflow = TestUtil.MOCK_WORKFLOW;

    @Test
    public void testGetWorkflowInput() {
        EventList events = TestUtil.loadActionEvents(Workflow.class, "SimpleWorkflowHistory.json");
        workflow.replaceEvents(events);
        assertEquals("100", workflow.getWorkflowInput());
    }

    @Test
    public void testGetWorkflowStartDate() {
        EventList events = TestUtil.loadActionEvents(Workflow.class, "SimpleWorkflowHistory.json");
        workflow.replaceEvents(events);
        assertEquals("Mon Apr 28 21:17:02 UTC 2014", workflow.getWorkflowStartDate().toString());
    }
}
