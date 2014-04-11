package com.clario.swift

import com.amazonaws.services.simpleworkflow.model.*
import com.amazonaws.services.simpleworkflow.model.transform.DecisionTaskJsonUnmarshaller
import com.amazonaws.services.simpleworkflow.model.transform.HistoryEventJsonUnmarshaller
import com.amazonaws.transform.JsonUnmarshallerContext
import com.amazonaws.transform.ListUnmarshaller
import com.amazonaws.transform.Unmarshaller
import com.fasterxml.jackson.core.JsonFactory
import com.fasterxml.jackson.core.JsonParser
import spock.lang.Specification

import static com.amazonaws.services.simpleworkflow.model.EventType.*

/**
 * @author George Coller
 */
public class HistoryInspectorTest extends Specification {

    static final DecisionTask unmarshalDecisionTask(String json) {
        Unmarshaller<DecisionTask, JsonUnmarshallerContext> unmarshaller = new DecisionTaskJsonUnmarshaller()
        JsonParser parser = new JsonFactory().createParser(json)
        unmarshaller.unmarshall(new JsonUnmarshallerContext(parser))
    }

    static final List<HistoryEvent> unmarshalHistoryEvents(String json) {
        JsonParser parser = new JsonFactory().createParser(json)
        new ListUnmarshaller<HistoryEvent>(HistoryEventJsonUnmarshaller.getInstance()).unmarshall(new JsonUnmarshallerContext(parser))
    }
}