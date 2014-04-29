package com.clario.swift;

import com.amazonaws.services.simpleworkflow.model.DecisionTask;
import com.amazonaws.services.simpleworkflow.model.transform.DecisionTaskJsonUnmarshaller;
import com.amazonaws.transform.JsonUnmarshallerContext;
import com.amazonaws.transform.Unmarshaller;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import org.junit.Test;

import java.nio.charset.Charset;
import java.nio.file.Path;
import java.nio.file.Paths;

import static com.clario.swift.SwiftUtil.join;
import static java.nio.file.Files.readAllLines;
import static org.junit.Assert.assertEquals;

public class WorkflowHistoryTest {
    static final DecisionTask unmarshalDecisionTask(String json) {
        try {
            Unmarshaller<DecisionTask, JsonUnmarshallerContext> unmarshaller = new DecisionTaskJsonUnmarshaller();
            JsonParser parser = new JsonFactory().createParser(json);
            return unmarshaller.unmarshall(new JsonUnmarshallerContext(parser));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    @Test
    public void testFilterEvents() throws Exception {
        WorkflowHistory history = loadWorkflow("SimpleWorkflowHistory.json");
        assertEquals(3, history.filterEvents("step1").size());
        assertEquals(3, history.filterEvents("step2").size());
        assertEquals(3, history.filterEvents("step3").size());
    }


    @Test
    public void testGetMarkers() throws Exception {
        WorkflowHistory history = loadWorkflow("RetryWorkflowHistory.json");
        assertEquals(1, history.getMarkers().size());
        assertEquals("1398724533227", history.getMarkers().get("failUntilTime"));
    }

    @Test
    public void testGetSignals() throws Exception {
        WorkflowHistory history = loadWorkflow("WaitForSignalWorkflow.json");
        assertEquals(1, history.getSignals().size());
        assertEquals("99", history.getSignals().get("Boo"));

    }

    @Test
    public void testGetWorkflowInput() throws Exception {
        WorkflowHistory history = loadWorkflow("SimpleWorkflowHistory.json");
        assertEquals("100", history.getWorkflowInput());
    }

    @Test
    public void testGetErrorEvents() throws Exception {
        WorkflowHistory history = loadWorkflow("ScheduleActivityTaskFailed.json");
        assertEquals(1, history.getErrorEvents().size());
    }

    public static WorkflowHistory loadWorkflow(String name) {
        try {
            Path p = Paths.get(WorkflowHistory.class.getResource(name).getPath());
            String json = join(readAllLines(p, Charset.defaultCharset()), "\n");
            WorkflowHistory history = new WorkflowHistory();
            history.addHistoryEvents(unmarshalDecisionTask(json).getEvents());
            return history;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}