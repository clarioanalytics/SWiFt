package com.clario.swift;

import com.amazonaws.services.simpleworkflow.model.RegisterWorkflowTypeRequest;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static java.lang.String.valueOf;

/**
 * Utility methods.
 *
 * @author George Coller
 */
public class SwiftUtil {
    public static final DateTimeFormatter DATE_TIME_MILLIS_FORMATTER = ISODateTimeFormat.dateTime().withZoneUTC();
    public static final int MAX_NUMBER_TAGS = 5;
    public static final int MAX_RUN_ID_LENGTH = 64;
    public static final int MAX_VERSION_LENGTH = 64;
    public static final int MAX_NAME_LENGTH = 256;
    public static final int MAX_ID_LENGTH = 256;
    public static final int MAX_REASON_LENGTH = 256;
    public static final int MAX_DESCRIPTION_LENGTH = 1024;
    public static final int MAX_INPUT_LENGTH = 32768;
    public static final int MAX_CONTROL_LENGTH = 32768;
    public static final int MAX_DETAILS_LENGTH = 32768;
    public static final int MAX_RESULT_LENGTH = 32768;

    // Ensure all-static utility class
    private SwiftUtil() { }

    /**
     * Assert the value passes the constraints for SWF fields like name, version, domain, taskList, identifiers.
     *
     * @param value to assert
     *
     * @return the parameter for method chaining
     * @see RegisterWorkflowTypeRequest#getName
     */
    public static String assertSwfValue(String value) {
        if (value != null) {
            if (value.length() == 0) {
                throw new AssertionError("Empty value not allowed");
            }
            if (value.length() == 0
                || value.matches("\\s.*|.*\\s")
                || value.matches(".*[:/|\\u0000-\\u001f\\u007f-\\u009f].*")
                || value.contains("arn")) {
                throw new AssertionError("Value contains one or more bad characters: '" + value + "'");
            }
        }
        return value;
    }

    /**
     * Assert that a string is less than or equal a maximum length.
     *
     * @param s string, null allowed
     * @param maxLength maximum length allowed
     *
     * @return s parameter for method chaining
     */
    public static String assertMaxLength(String s, int maxLength) {
        if (s != null && s.length() > maxLength) {
            throw new AssertionError(format("String length %d > max length %d", s.length(), maxLength));
        }
        return s;
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
        try {
            if (s != null && s.length() > maxLength) {
                return s.substring(0, maxLength);
            } else {
                return s;
            }
        } catch (StringIndexOutOfBoundsException e) {
            throw new IllegalArgumentException(format("trimToMaxLength(%s, %d)", s, maxLength));
        }
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
     * @return true if the parameter is not null or has a length greater than zero
     */
    public static boolean isNotEmpty(String s) {
        return !(s == null || s.length() == 0);
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
     * Combine a name and version into a single string for easier indexing in maps, etc.
     * In SWF registered workflows and activities are identified by the combination of name and version.
     */
    public static String makeKey(String name, String version) {
        return format("%s-%s", name, version);
    }

    /**
     * Utility method to convert a stack trace to a String
     */
    public static String printStackTrace(final Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

    /**
     * Create a millisecond-accurate ISO timestamp using current time;
     */
    public static String timestamp() {
        return DATE_TIME_MILLIS_FORMATTER.print(System.currentTimeMillis());
    }

    /** SWF value for timeouts to indicate no-timeout value */
    public static final String TIMEOUT_NONE = "NONE";

    /**
     * Calc a SWF timeout string.
     * Pass null unit or duration &lt;= 0 for a timeout of NONE.
     *
     * @param unit time unit to use with duration
     * @param duration duration converted to seconds
     */
    public static String calcTimeoutString(TimeUnit unit, long duration) {
        return unit == null || duration < 1 ? TIMEOUT_NONE : valueOf(unit.toSeconds(duration));
    }

    /**
     * Make a unique and valid workflowId.
     * Replaces bad characters and whitespace, which also makes it easy for amazon cli use.
     *
     * @param workflowName name of workflow.
     *
     * @return unique workflowId
     */
    public static String createUniqueWorkflowId(String workflowName) {
        String name = workflowName.trim()
            .replaceAll("\\s|[:/|\\u0000-\\u001f\\u007f-\\u009f]", "_")
            .replaceAll("arn", "Arn");
        String timestamp = "." + timestamp().replaceAll(":", ".");
        name = trimToMaxLength(name, MAX_ID_LENGTH - timestamp.length());
        return assertSwfValue(name + timestamp);
    }
}
