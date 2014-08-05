package com.clario.swift;

import com.amazonaws.services.simpleworkflow.model.*;
import com.amazonaws.services.simpleworkflow.model.transform.DecisionTaskJsonUnmarshaller;
import com.amazonaws.transform.JsonUnmarshallerContext;
import com.amazonaws.transform.Unmarshaller;
import com.clario.swift.action.ActivityAction;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;

import java.io.FileNotFoundException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.amazonaws.services.simpleworkflow.model.DecisionType.FailWorkflowExecution;
import static com.amazonaws.services.simpleworkflow.model.EventType.ActivityTaskCompleted;
import static com.amazonaws.services.simpleworkflow.model.EventType.ActivityTaskFailed;
import static com.clario.swift.Workflow.createFailReasonString;
import static java.lang.String.format;
import static java.nio.charset.Charset.defaultCharset;
import static java.nio.file.Files.readAllLines;

/**
 * Utility class for mocking and unit testing the decision path of a workflow.
 * <p/>
 * The class is written so that the history events only need to be loaded once and then
 * various testing subsets can be created with {@link #getEvents} after calling zero or more
 * of the provided filtering methods (methods prefixed by 'with')
 * <p/>
 * See the unit tests for the example workflows in <code>com.clario.swift.examples.workflows</code>
 *
 * @author George Coller
 */
public class MockWorkflowHistory {

    private List<HistoryEvent> historyEvents = new ArrayList<HistoryEvent>();
    private List<Long> includeEvents = new ArrayList<Long>();
    private List<Long> excludeEvents = new ArrayList<Long>();
    private ActivityTaskFailedEventAttributes activityTaskFailedEventAttributes;
    private Long startEventId = null;
    private Long endEventId = null;

    private EventType stopAtEventType;
    private Integer stopAfterCount;

    /**
     * Keep history events up to and including the nth occurrence of the given {@link EventType}
     * in the list returned by {@link #getEvents()}.
     *
     * @param eventType event type
     * @param count number of times to include
     *
     * @see #withStopAtDecisionTaskStarted
     */
    public MockWorkflowHistory withStopAtEventType(EventType eventType, int count) {
        stopAtEventType = eventType;
        stopAfterCount = count;
        return this;
    }

    /**
     * Specialization of {@link #withStopAtEventType} with {@link EventType#DecisionTaskStarted} as the event type.
     */
    public MockWorkflowHistory withStopAtDecisionTaskStarted(int count) {
        return withStopAtEventType(EventType.DecisionTaskStarted, count);
    }

    /**
     * Select one or more event identifiers to include in the list returned by {@link #getEvents()}.
     *
     * @param eventIds one or more event ids.
     */
    public MockWorkflowHistory withEventIds(long... eventIds) {
        for (long eventId : eventIds) {
            includeEvents.add(eventId);
        }
        return this;
    }

    /**
     * Exclude one or more event identifiers from the list returned by {@link #getEvents()}.
     */
    public MockWorkflowHistory withoutEventIds(long... eventIds) {
        for (long eventId : eventIds) {
            excludeEvents.add(eventId);
        }
        return this;
    }

    /**
     * Use an event id range to filter the list returned by {@link #getEvents()}.
     *
     * @param startEventId minimum event id in range.
     * @param endEventId maximum eventId in range.
     */
    public MockWorkflowHistory withEventIdInRange(long startEventId, long endEventId) {
        this.startEventId = startEventId;
        this.endEventId = endEventId;
        return this;
    }

    /**
     * Convert the most recent action returned by {@link #getEvents()} to its
     * related failed version with the given parameters.
     */
    public MockWorkflowHistory withConvertLastActivityTaskToFailed(String reason, String details) {
        activityTaskFailedEventAttributes = new ActivityTaskFailedEventAttributes()
            .withDetails(details)
            .withReason(reason);
        return this;
    }

    /**
     * Clear any filters so that {@link #getEvents()} returns all loaded events.
     */
    public void resetEventFilters() {
        includeEvents = new ArrayList<Long>();
        excludeEvents = new ArrayList<Long>();
        activityTaskFailedEventAttributes = null;
        startEventId = null;
        endEventId = null;
    }

    /**
     * Return a list of {@link HistoryEvent} filtered by any previously-called filter methods (methods prefixed by "with").
     *
     * @see #resetEventFilters()
     */
    public List<HistoryEvent> getEvents() {
        List<HistoryEvent> result = new ArrayList<HistoryEvent>();
        int decisionTaskCount = 0;

        for (HistoryEvent historyEvent : historyEvents) {
            Long eventId = historyEvent.getEventId();

            if (startEventId == null || eventId >= startEventId) {
                if (endEventId == null || eventId <= endEventId) {
                    if (includeEvents.isEmpty() || includeEvents.contains(eventId)) {
                        if (excludeEvents.isEmpty() || !excludeEvents.contains(eventId)) {
                            result.add(historyEvent);
                        }
                    }
                }
            }
            if (stopAtEventType != null && equals(historyEvent, stopAtEventType)) {
                decisionTaskCount++;
                if (decisionTaskCount >= stopAfterCount) {
                    break;
                }
            }

        }
        sortHistoryEventsDescending(result);
        convertTaskFailed(result);
        return result;
    }

    protected void convertTaskFailed(List<HistoryEvent> result) {
        if (activityTaskFailedEventAttributes != null) {
            for (int i = 0; i < result.size(); i++) {
                HistoryEvent historyEvent = result.get(i);
                if (equals(historyEvent, ActivityTaskCompleted)) {
                    assertEqualsToString(ActivityTaskCompleted, historyEvent.getEventType());
                    result.add(i, new HistoryEvent()
                        .withEventType(ActivityTaskFailed)
                        .withEventId(historyEvent.getEventId())
                        .withEventTimestamp(historyEvent.getEventTimestamp())
                        .withActivityTaskFailedEventAttributes(activityTaskFailedEventAttributes
                            .withScheduledEventId(historyEvent.getActivityTaskCompletedEventAttributes().getScheduledEventId())
                            .withStartedEventId(historyEvent.getActivityTaskCompletedEventAttributes().getStartedEventId())));
                    break;
                }
            }
        }
    }

    /**
     * Load history events from a json-formatted file.
     * Note: json is expected to be in the native format used by SWF
     */
    public void loadEvents(Class locationClass, String name) {
        resetEventFilters();
        historyEvents = new ArrayList<HistoryEvent>();
        historyEvents.addAll(loadHistoryEventList(locationClass, name));
        sortHistoryEventsAscending(historyEvents);
    }

    /**
     * Parse the given json into a list of history events.
     * Note: json is expected to be in the native format used by SWF
     */
    public void loadEvents(String json) {
        resetEventFilters();
        historyEvents = new ArrayList<HistoryEvent>();
        historyEvents.addAll(unmarshalDecisionTask(json).getEvents());
        sortHistoryEventsAscending(historyEvents);
    }

    /**
     * Use SWF API to unmarshal a json document into a {@link DecisionTask}.
     * Note: json is expected to be in the native format used by SWF
     */
    public static DecisionTask unmarshalDecisionTask(String json) {
        try {
            Unmarshaller<DecisionTask, JsonUnmarshallerContext> unmarshaller = new DecisionTaskJsonUnmarshaller();
            JsonParser parser = new JsonFactory().createParser(json);
            return unmarshaller.unmarshall(new JsonUnmarshallerContext(parser));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Load workflow history from a json data file that lives in the same package as a given class into a list of {@link HistoryEvent}.
     * Sorted descending by evend id
     *
     * @param locationClass class that lives in the same package as the file.
     * @param name name of the json data file.
     *
     * @return list of history events
     */
    public static List<HistoryEvent> loadHistoryEventList(Class locationClass, String name) {
        try {
            List<HistoryEvent> historyEvents = new ArrayList<HistoryEvent>();
            DecisionTask decisionTask = unmarshalDecisionTask(readFile(locationClass, name));
            historyEvents.addAll(decisionTask.getEvents());
            sortHistoryEventsDescending(historyEvents);
            return historyEvents;
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Utility method to sort a list of {@link HistoryEvent} in descending event id order.
     */
    public static void sortHistoryEventsDescending(List<HistoryEvent> historyEvents) {
        Collections.sort(historyEvents, new Comparator<HistoryEvent>() {
            public int compare(HistoryEvent he1, HistoryEvent he2) {
                return -he1.getEventId().compareTo(he2.getEventId());
            }
        });
    }

    /**
     * Utility method to sort a list of {@link HistoryEvent} in ascending event id order.
     */
    public static void sortHistoryEventsAscending(List<HistoryEvent> historyEvents) {
        Collections.sort(historyEvents, new Comparator<HistoryEvent>() {
            public int compare(HistoryEvent he1, HistoryEvent he2) {
                return he1.getEventId().compareTo(he2.getEventId());
            }
        });
    }


    /**
     * Read a file on the classpath into a string.
     *
     * @param clazz class in the same package as the file to be read in.
     * @param fileName name of file
     *
     * @return file contents as a string
     */
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

    public static boolean equals(HistoryEvent historyEvent, EventType eventType) {
        return eventType.toString().equals(historyEvent.getEventType());
    }

    public static boolean equals(Decision decision, DecisionType type) {
        return type.toString().equals(decision.getDecisionType());
    }

    public static void assertEquals(Decision decision, DecisionType decisionType) {
        assertEqualsToString(decisionType, decision.getDecisionType());
    }

    public static void assertEquals(HistoryEvent historyEvent, EventType eventType) {
        assertEqualsToString(eventType, historyEvent.getEventType());
    }

    public static void assertEqualsToString(Object expected, Object actual) {
        if (!expected.toString().equals(actual.toString())) {
            throw new AssertionError(format("ComparisonFailure\nexpected:<%s> but was:<%s>", expected, actual));
        }
    }

    /**
     * Assert that a workflow failed because of the given action.
     */
    public static void assertActionCausedWorkflowExecutionDecision(ActivityAction action, Decision decision, String reason, String detail) {
        assertEquals(decision, FailWorkflowExecution);
        String expectedReason = createFailReasonString(action.toString(), reason);
        FailWorkflowExecutionDecisionAttributes attributes = decision.getFailWorkflowExecutionDecisionAttributes();
        if (!expectedReason.equals(attributes.getReason())) {
            throw new AssertionError(format("ComparisonFailure\nexpected:<%s> but was:<%s>", expectedReason, attributes.getReason()));
        }
        if (detail != null && !detail.equals(attributes.getDetails())) {
            throw new AssertionError(format("ComparisonFailure\nexpected:<%s> but was:<%s>", detail, attributes.getDetails()));
        }
    }

}
