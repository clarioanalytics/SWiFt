package com.clario.swift;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.clario.swift.action.ActivityAction;
import org.junit.Test;

import java.util.List;

import static com.amazonaws.services.simpleworkflow.model.ChildPolicy.TERMINATE;
import static com.clario.swift.SwiftUtil.*;
import static org.junit.Assert.assertEquals;

/**
 * @author George Coller
 */
public class TestDefaultValues {

    @Test
    public void testWorkflowDefaults() {
        Workflow w = new Workflow("Mock Workflow", "1") {
            @Override public void decide(List<Decision> decisions) {

            }
        };
        assertEquals(SWF_TIMEOUT_DECISION_DEFAULT, w.getTaskStartToCloseTimeout());
        assertEquals(SWF_TIMEOUT_YEAR, w.getExecutionStartToCloseTimeout());
        assertEquals(TERMINATE, w.getChildPolicy());
        assertEquals("Mock Workflow-1", w.getKey());
    }

    @Test
    public void testActionDefaults() {
        ActivityAction action = new ActivityAction("id123", "Mock Activity", "1.0");
        assertEquals(SWF_TIMEOUT_NONE, action.getHeartBeatTimeoutTimeout());
        assertEquals(SWF_TIMEOUT_NONE, action.getScheduleToCloseTimeout());
        assertEquals(SWF_TIMEOUT_NONE, action.getScheduleToStartTimeout());
        assertEquals(SWF_TIMEOUT_NONE, action.getStartToCloseTimeout());
        assertEquals("ActivityAction id123 Mock Activity-1.0", action.toString());
    }

    @Test
    public void testUnsetDefaultTimeouts() {
        ActivityAction action = new ActivityAction("id123", "Mock Activity", "1.0");
        action.withUnsetDefaultTimeouts();
        assertEquals(null, action.getHeartBeatTimeoutTimeout());
        assertEquals(null, action.getScheduleToCloseTimeout());
        assertEquals(null, action.getScheduleToStartTimeout());
        assertEquals(null, action.getStartToCloseTimeout());
    }
}
