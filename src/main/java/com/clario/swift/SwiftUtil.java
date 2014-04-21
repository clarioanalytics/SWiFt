package com.clario.swift;

import com.amazonaws.services.simpleworkflow.model.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static java.lang.String.format;
import static java.lang.String.valueOf;

/**
 * Utility methods.
 *
 * @author George Coller
 */
public class SwiftUtil {
    public static final ObjectMapper JSON_OBJECT_MAPPER = new ObjectMapper();
    public static final int MAX_REASON_LENGTH = 256;
    public static final int MAX_DETAILS_LENGTH = 32768;
    public static final int MAX_RESULT_LENGTH = 32768;

    // Ensure all-static utility class
    private SwiftUtil() { }

    /**
     * Convert an object into a JSON string.
     *
     * @param o object to convert
     *
     * @return JSON string.
     * @see com.fasterxml.jackson.databind.ObjectMapper for details on what can be converted.
     */
    public static String toJson(Object o) {
        try {
            return JSON_OBJECT_MAPPER.writeValueAsString(o);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize to JSON", e);
        }
    }

    /**
     * Convert a JSON string into a structure of Java collections.
     *
     * @param json string to convert
     *
     * @return converted structure
     * @see com.fasterxml.jackson.databind.ObjectMapper for details on what can be converted.
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> fromJson(String json) {
        try {
            return JSON_OBJECT_MAPPER.readValue(json, Map.class);
        } catch (IOException e) {
            throw new IllegalStateException(format("Failed to unmarshal JSON: \"%s\"", json), e);
        }
    }

    /**
     * Trim a string if it exceeds a maximum length.
     *
     * @param s string to trim, null allowed
     * @param maxLength max length
     *
     * @return trimmed string if it exceeded maximum length, otherwise string parameter
     */
    public static String trimToMaxLength(String s, int maxLength) {
        if (s != null && s.length() > maxLength) {
            return s.substring(0, maxLength - 1);
        } else {
            return s;
        }
    }

    /**
     * @return true if the parameter is not null or has a length greater than zero
     */
    public static boolean isNotEmpty(String s) {
        return !(s == null || s.length() == 0);
    }

    /**
     * @return replacement parameter if the value parameter is null, otherwise return value parameter.
     */
    public static <T> T defaultIfNull(T value, T replacement) {
        return value == null ? replacement : value;
    }

    /**
     * @return replacement parameter converted to a string if the value string is null or empty, otherwise return value string.
     */
    public static <T> String defaultIfEmpty(String value, T replacement) {
        if (isNotEmpty(value)) {
            return value;
        } else {
            return replacement == null ? null : valueOf(replacement);
        }
    }

    /**
     * Convert a collection of items into a string with each item separated by the separator parameter.
     *
     * @param items collection to join
     * @param separator string to insert between each item, defaults to empty string.
     *
     * @return joined string
     */
    public static <T> String join(Collection<T> items, String separator) {
        separator = defaultIfNull(separator, "");
        int size = items.size();
        StringBuilder b = new StringBuilder((10 + separator.length()) * items.size());
        int i = 0;
        for (T item : items) {
            b.append(item);
            i++;
            if (i < size) {
                b.append(separator);
            }
        }
        return b.toString();
    }

    /**
     * Join each entry of a map into a string using a given separator and returning the resulting list of strings.
     *
     * @param map map to join
     * @param separator string to insert between each key,value pair, defaults to empty string.
     *
     * @return list of joined entries
     */
    public static <A, B> List<String> joinEntries(Map<A, B> map, String separator) {
        List<String> list = new ArrayList<>(map.size());
        separator = defaultIfNull(separator, "");
        for (Map.Entry<A, B> entry : map.entrySet()) {
            list.add(format("%s%s%s", defaultIfNull(entry.getKey(), ""), separator, defaultIfNull(entry.getValue(), "")));
        }
        return list;
    }

    /**
     * Combine a name and version into a single string for easier indexing in maps, etc.
     * In SWF registered workflows and activities are identified by the combination of name and version.
     */
    public static String makeKey(String name, String version) {
        return name + "-" + version;
    }

    /**
     * Utility method to convert a stack trace to a String
     */
    public static String printStackTrace(final Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    //
    // Amazon SWF request helpers
    //

    public static RecordActivityTaskHeartbeatRequest createRecordActivityTaskHeartbeat(String taskToken, String details) {
        return new RecordActivityTaskHeartbeatRequest()
            .withTaskToken(taskToken)
            .withDetails(trimToMaxLength(details, MAX_DETAILS_LENGTH));
    }

    public static PollForActivityTaskRequest createPollForActivityTask(String domain, String taskList, String id) {
        return new PollForActivityTaskRequest()
            .withDomain(domain)
            .withTaskList(new TaskList()
                .withName(taskList))
            .withIdentity(id);
    }

    public static RespondActivityTaskFailedRequest createRespondActivityTaskFailed(String taskToken, String reason, String details) {
        return new RespondActivityTaskFailedRequest()
            .withTaskToken(taskToken)
            .withReason(trimToMaxLength(reason, MAX_REASON_LENGTH))
            .withDetails(trimToMaxLength(details, MAX_DETAILS_LENGTH));
    }

    public static RespondActivityTaskCompletedRequest createRespondActivityCompleted(ActivityTask task, String result) {
        return new RespondActivityTaskCompletedRequest()
            .withTaskToken(task.getTaskToken())
            .withResult(trimToMaxLength(result, MAX_RESULT_LENGTH));
    }

    public static Decision createCompleteWorkflowExecutionDecision(String result) {
        return new Decision()
            .withDecisionType(DecisionType.CompleteWorkflowExecution)
            .withCompleteWorkflowExecutionDecisionAttributes(
                new CompleteWorkflowExecutionDecisionAttributes()
                    .withResult(trimToMaxLength(result, MAX_RESULT_LENGTH))
            );
    }

    public static Decision createFailWorkflowExecutionDecision(String reason, String details) {
        return new Decision()
            .withDecisionType(DecisionType.FailWorkflowExecution)
            .withFailWorkflowExecutionDecisionAttributes(
                new FailWorkflowExecutionDecisionAttributes()
                    .withReason(trimToMaxLength(reason, MAX_REASON_LENGTH))
                    .withDetails(trimToMaxLength(details, MAX_DETAILS_LENGTH))
            );
    }

    public static Decision createCancelActivityDecision(String id) {
        return new Decision()
            .withDecisionType(DecisionType.RequestCancelActivityTask)
            .withRequestCancelActivityTaskDecisionAttributes(
                new RequestCancelActivityTaskDecisionAttributes().withActivityId(id)
            );
    }

    public static Decision createScheduleActivityTaskDecision(
        String activityId,
        String name,
        String version,
        String taskList,
        String input,
        String control,
        String heartBeatTimeoutTimeout,
        String scheduleToCloseTimeout,
        String scheduleToStartTimeout,
        String startToCloseTimeout
    ) {
        return new Decision()
            .withDecisionType(DecisionType.ScheduleActivityTask)
            .withScheduleActivityTaskDecisionAttributes(new ScheduleActivityTaskDecisionAttributes()
                .withActivityType(new ActivityType()
                    .withName(name)
                    .withVersion(defaultIfNull(version, "1.0")))
                .withActivityId(activityId)
                .withTaskList(new TaskList()
                    .withName(defaultIfNull(taskList, "default")))
                .withInput(defaultIfNull(input, ""))
                .withControl(defaultIfNull(control, ""))
                .withHeartbeatTimeout(heartBeatTimeoutTimeout)
                .withScheduleToCloseTimeout(scheduleToCloseTimeout)
                .withScheduleToStartTimeout(scheduleToStartTimeout)
                .withStartToCloseTimeout(startToCloseTimeout));
    }


    public static RegisterActivityTypeRequest createRegisterActivityType(
        String domain,
        String taskList,
        String name,
        String version,
        String description,
        String heartBeatTimeoutTimeout,
        String startToCloseTimeout,
        String scheduleToStartTimeout,
        String scheduleToCloseTimeout
    ) {
        return new RegisterActivityTypeRequest()
            .withDomain(domain)
            .withDefaultTaskList(new TaskList().withName(taskList))
            .withName(name)
            .withVersion(version)
            .withDescription(defaultIfEmpty(description, null))
            .withDefaultTaskHeartbeatTimeout(defaultIfEmpty(heartBeatTimeoutTimeout, null))
            .withDefaultTaskStartToCloseTimeout(defaultIfEmpty(startToCloseTimeout, null))
            .withDefaultTaskScheduleToStartTimeout(defaultIfEmpty(scheduleToStartTimeout, null))
            .withDefaultTaskScheduleToCloseTimeout(defaultIfEmpty(scheduleToCloseTimeout, null));
    }

}
