package com.clario.swift;

import com.clario.swift.examples.workflows.TimerWorkflow;
import org.junit.Assert;
import org.junit.Test;

/**
 * @author George Coller
 */
public class DecisionPollerTest {


    @Test
    public void testAddDuplicateWorkflowInstance() {
        DecisionPoller poller1 = new DecisionPoller("poller1", "domain", "taskList", "context");
        DecisionPoller poller2 = new DecisionPoller("poller2", "domain", "taskList", "context");

        Workflow wf = new TimerWorkflow();
        poller1.addWorkflows(wf);
        try {
            poller2.addWorkflows(wf);
        } catch (IllegalStateException e) {
            Assert.assertEquals("Attempt to add same instance of workflow Timer Workflow-1.0 to multiple decision pollers", e.getMessage());
            return;
        }
        Assert.fail("Expecting exception");
    }
}
