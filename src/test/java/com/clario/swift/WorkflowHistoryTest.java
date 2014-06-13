package com.clario.swift;

import com.amazonaws.services.simpleworkflow.model.DecisionTask;
import com.amazonaws.services.simpleworkflow.model.transform.DecisionTaskJsonUnmarshaller;
import com.amazonaws.transform.JsonUnmarshallerContext;
import com.amazonaws.transform.Unmarshaller;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import org.junit.Test;

import java.io.FileNotFoundException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import static java.lang.String.format;
import static java.nio.charset.Charset.defaultCharset;
import static java.nio.file.Files.readAllLines;
import static org.junit.Assert.assertEquals;

public class WorkflowHistoryTest {
    static DecisionTask unmarshalDecisionTask(String json) {
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
        assertEquals(3, history.filterActionEvents("step1").size());
        assertEquals(3, history.filterActionEvents("step2").size());
        assertEquals(3, history.filterActionEvents("step3").size());
    }


    @Test
    public void testGetMarkers() throws Exception {
        WorkflowHistory history = loadWorkflow("RetryWorkflowHistory.json");
        List<ActionEvent> markers = history.getMarkers();
        assertEquals(1, markers.size());
        assertEquals("failUntilTime", markers.get(0).getActionId());
        assertEquals("1398724533227", markers.get(0).getData1());
    }

    @Test
    public void testGetSignals() throws Exception {
        WorkflowHistory history = loadWorkflow("WaitForSignalWorkflow.json");
        List<ActionEvent> signals = history.getSignals();
        assertEquals(1, signals.size());
        assertEquals("Boo", signals.get(0).getActionId());
        assertEquals("99", signals.get(0).getData1());
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
            WorkflowHistory history = new WorkflowHistory();
            history.addHistoryEvents(unmarshalDecisionTask(readFile(WorkflowHistory.class, name)).getEvents());
            return history;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    public static String readFile(Class clazz, String fileName) {
        try {
            URL resource = clazz.getResource(fileName);
            if (resource == null) {
                throw new FileNotFoundException(format("%s.class.getResource(\"%s\") returned null", clazz.getName(), fileName));
            }
            Path p = Paths.get(resource.getPath());
            return SwiftUtil.join(readAllLines(p, defaultCharset()), "\n");
        } catch (Exception e) {
            throw new IllegalArgumentException(format("Error reading file \"%s\"", fileName), e);
        }
    }
}