package com.clario.swift;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.amazonaws.services.simpleworkflow.model.DecisionType;
import com.clario.swift.examples.workflows.TimerWorkflow;
import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

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

    @Test
    public void testCheckFailWorkflowExecutionDecisionExists() {
        DecisionPoller poller = new DecisionPoller("poller", "domain", "taskList", "context");
        List<Decision> decisions = new ArrayList<>();
        Decision failWorkflowDecision = new Decision().withDecisionType(DecisionType.FailWorkflowExecution);
        decisions.add(new Decision().withDecisionType(DecisionType.FailWorkflowExecution));
        decisions.add(new Decision().withDecisionType(DecisionType.StartTimer));
        decisions.add(failWorkflowDecision);

        poller.checkFailWorkflowExecutionDecision(decisions);
        Assert.assertEquals("Expect exactly one decision", 1, decisions.size());
        Assert.assertEquals("Expect last-added fail decision", failWorkflowDecision, decisions.get(0));
    }

    @Test
    public void testCheckFailWorkflowExecutionDecisionNotExists() {
        DecisionPoller poller = new DecisionPoller("poller", "domain", "taskList", "context");
        List<Decision> decisions = new ArrayList<>();
        decisions.add(new Decision().withDecisionType(DecisionType.StartTimer));
        decisions.add(new Decision().withDecisionType(DecisionType.RecordMarker));
        decisions.add(new Decision().withDecisionType(DecisionType.ScheduleActivityTask));
        List<Decision> expected = new ArrayList<>(decisions);

        poller.checkFailWorkflowExecutionDecision(decisions);
        Assert.assertEquals("Expect no change in decision list", expected, decisions);
    }
}
