package com.clario.swift;

import com.amazonaws.services.simpleworkflow.model.DecisionTask;
import com.amazonaws.services.simpleworkflow.model.HistoryEvent;
import com.amazonaws.services.simpleworkflow.model.transform.DecisionTaskJsonUnmarshaller;
import com.amazonaws.services.simpleworkflow.model.transform.HistoryEventJsonUnmarshaller;
import com.amazonaws.transform.JsonUnmarshallerContext;
import com.amazonaws.transform.ListUnmarshaller;
import com.amazonaws.transform.Unmarshaller;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;

import java.util.List;

/**
 * @author George Coller
 */
public class HistoryInspectorTest {
    public static DecisionTask unmarshalDecisionTask(String json) throws Exception {
        Unmarshaller<DecisionTask, JsonUnmarshallerContext> unmarshaller = new DecisionTaskJsonUnmarshaller();
        JsonParser parser = new JsonFactory().createParser(json);
        return unmarshaller.unmarshall(new JsonUnmarshallerContext(parser));
    }

    public static List<HistoryEvent> unmarshalHistoryEvents(String json) throws Exception {
        JsonParser parser = new JsonFactory().createParser(json);
        return new ListUnmarshaller<>(HistoryEventJsonUnmarshaller.getInstance()).unmarshall(new JsonUnmarshallerContext(parser));
    }

}
