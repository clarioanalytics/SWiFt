package com.clario.swift;

import com.amazonaws.services.simpleworkflow.model.*;
import com.amazonaws.services.simpleworkflow.model.transform.DecisionTaskJsonUnmarshaller;
import com.amazonaws.transform.JsonUnmarshallerContext;
import com.amazonaws.transform.JsonUnmarshallerContextImpl;
import com.amazonaws.transform.Unmarshaller;
import com.clario.swift.action.ActivityAction;
import com.clario.swift.event.Event;
import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;

import java.io.FileNotFoundException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static com.amazonaws.services.simpleworkflow.model.DecisionType.FailWorkflowExecution;
import static com.amazonaws.services.simpleworkflow.model.EventType.ActivityTaskCompleted;
import static com.amazonaws.services.simpleworkflow.model.EventType.ActivityTaskFailed;
import static com.clario.swift.EventList.convert;
import static com.clario.swift.Workflow.createFailReasonString;
import static java.lang.String.format;
import static java.nio.charset.Charset.defaultCharset;
import static java.nio.file.Files.readAllLines;

/**
 * Utility class for unit testing.
 *
 * @author George Coller
 */
public class TestUtil {

    private TestUtil() {
        // ensure all-static utility class
    }

    public static Workflow MOCK_WORKFLOW = new Workflow("Mock Workflow", "1.0") {
        @Override public void decide(List<Decision> decisions) {
            // do nothing
        }
    };

    public static void convertTaskFailed(List<Event> historyEvents, String reason, String details) {
        ActivityTaskFailedEventAttributes eventAttributes =
            new ActivityTaskFailedEventAttributes()
                .withDetails(details)
                .withReason(reason);
        for (int i = 0; i < historyEvents.size(); i++) {
            Event actionEvent = historyEvents.get(i);
            if (equals(actionEvent, ActivityTaskCompleted)) {
                HistoryEvent historyEvent = new HistoryEvent()
                    .withEventType(ActivityTaskFailed)
                    .withEventId(actionEvent.getEventId())
                    .withEventTimestamp(actionEvent.getEventTimestamp().toDate())
                    .withActivityTaskFailedEventAttributes(eventAttributes
                        .withScheduledEventId(actionEvent.getHistoryEvent().getActivityTaskCompletedEventAttributes().getScheduledEventId())
                        .withStartedEventId(actionEvent.getHistoryEvent().getActivityTaskCompletedEventAttributes().getStartedEventId()));

                historyEvents.add(i, new Event(historyEvent));
                break;
            }
        }
    }

    public static void resetTimestampsStartingAt(List<Event> historyEvents, Date timestamp) {
        Date firstTimestamp = null;

        for (Event actionEvent : historyEvents) {
            HistoryEvent historyEvent = actionEvent.getHistoryEvent();
            if (firstTimestamp == null) {
                firstTimestamp = historyEvent.getEventTimestamp();
                historyEvent.setEventTimestamp(timestamp);
            } else {
                long millisBetween = historyEvent.getEventTimestamp().getTime() - firstTimestamp.getTime();
                assert millisBetween >= 0;
                historyEvent.setEventTimestamp(new Date(timestamp.getTime() + millisBetween));
            }
        }
    }

    /**
     * Use SWF API to unmarshal a json document into a {@link DecisionTask}.
     * Note: json is expected to be in the native format used by SWF
     */
    public static DecisionTask unmarshalDecisionTask(String json) {
        try {
            Unmarshaller<DecisionTask, JsonUnmarshallerContext> unmarshaller = new DecisionTaskJsonUnmarshaller();
            JsonParser parser = new JsonFactory().createParser(json);
            return unmarshaller.unmarshall(new JsonUnmarshallerContextImpl(parser));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Load workflow history converted to a list of {@link Event} sorted in descending event id order
     * from a json-formatted data file.
     *
     * @param clazz class that lives in the same package as the file.
     * @param fileName name of the json data file.
     *
     * @return list of history events
     */
    public static EventList loadActionEvents(Class clazz, String fileName) {
        return parseActionEvents(readFile(clazz, fileName));
    }

    /**
     * Parse workflow history from a json-formatted string into a list of {@link Event} sorted in descending event id order.
     * <p/>
     * Note: json format is same as native format used by Amazon SWF responses.
     *
     * @param json json to parse
     */
    public static EventList parseActionEvents(String json) {
        List<HistoryEvent> historyEvents = new ArrayList<HistoryEvent>();
        historyEvents.addAll(unmarshalDecisionTask(json).getEvents());
        return convert(historyEvents);
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

    public static boolean equals(Event historyEvent, EventType eventType) {
        return historyEvent.getType() == eventType;
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

    /**
     * Select events up to the nth occurrence of a given {@link EventType}.
     *
     * @param eventType event type
     * @param times number of occurrence to allow before excluding
     */
    public static SelectFunction byEventTypeTimes(final EventType eventType, final int times) {
        return new SelectFunction() {
            Long occurrenceEventId = -1L;

            public boolean select(Event event, int index, EventList eventList) {
                if (index == 0) {
                    int occurrenceCount = 0;
                    for (int i = eventList.size() - 1; i >= 0; i--) {
                        Event ev = eventList.get(i);
                        if (eventType == ev.getType()) {
                            occurrenceCount++;
                            if (occurrenceCount == times) {
                                occurrenceEventId = ev.getEventId();
                                break;
                            }
                        }
                    }
                }
                return event.getEventId() <= occurrenceEventId;
            }
        };
    }

    /**
     * Specialization of {@link #byEventTypeTimes} with {@link EventType#DecisionTaskStarted} as the event type.
     */
    public static SelectFunction byUpToDecision(int times) {
        return byEventTypeTimes(EventType.DecisionTaskStarted, times);
    }

    /**
     * convert the most recent ActivityTaskCompleted to ActivityTaskFailed in the EventList.
     *
     * @param eventList list of events
     * @param actionId actionId optional, if provided will limit the search by action id
     * @param reason reason
     * @param details details
     *
     * @return new EventList with converted activity
     */
    public static EventList convertActivitySuccessToFail(EventList eventList, String actionId, String reason, String details) {
        ActivityTaskFailedEventAttributes eventAttributes =
            new ActivityTaskFailedEventAttributes()
                .withDetails(details)
                .withReason(reason);

        List<Event> converted = new ArrayList<Event>();
        HistoryEvent historyEvent = null;
        for (Event event : eventList) {
            if (historyEvent == null && event.getType() == ActivityTaskCompleted && (actionId == null || actionId.equals(event.getActionId()))) {
                historyEvent = new HistoryEvent()
                    .withEventType(ActivityTaskFailed)
                    .withEventId(event.getEventId())
                    .withEventTimestamp(event.getEventTimestamp().toDate())
                    .withActivityTaskFailedEventAttributes(eventAttributes
                        .withScheduledEventId(event.getHistoryEvent().getActivityTaskCompletedEventAttributes().getScheduledEventId())
                        .withStartedEventId(event.getHistoryEvent().getActivityTaskCompletedEventAttributes().getStartedEventId()));

                converted.add(new Event(historyEvent));
            } else {
                converted.add(event);
            }
        }
        return new EventList(converted);
    }
}
