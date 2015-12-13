package com.clario.swift;

import com.amazonaws.services.simpleworkflow.model.RegisterWorkflowTypeRequest;
import org.joda.time.format.DateTimeFormatter;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collection;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.util.concurrent.TimeUnit.DAYS;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.joda.time.format.ISODateTimeFormat.dateTime;

/**
 * Utility methods.
 *
 * @author George Coller
 */
public class SwiftUtil {
    public static final String SWF_TIMEOUT_NONE = "NONE";
    public static final String SWF_TIMEOUT_YEAR = valueOf((DAYS.toSeconds(365)));
    public static final String SWF_TIMEOUT_DECISION_DEFAULT = valueOf(MINUTES.toSeconds(1));
    public static final DateTimeFormatter DATE_TIME_MILLIS_FORMATTER = dateTime().withZoneUTC();
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
     * Calc a SWF timeout string.
     * Pass null unit or duration &lt;= 0 for a timeout of NONE.
     *
     * @param unit time unit to use with duration
     * @param duration duration converted to seconds
     *
     * @see #calcTimeoutOrYear(TimeUnit, long)
     */
    public static String calcTimeoutOrNone(TimeUnit unit, long duration) {
        return unit == null || duration < 1 ? SWF_TIMEOUT_NONE : valueOf(unit.toSeconds(duration));
    }

    /**
     * Calc a SWF timeout string.
     * Pass null unit or duration &lt;= 0 for a timeout of 365 days.
     * <p/>
     * Some SWF timeouts, specifically workflow execution start to close timeouts cannot be set to "NONE".
     * Instead a maximum duration of 365 days is used for the default.
     *
     * @param unit time unit to use with duration
     * @param duration duration converted to seconds
     *
     * @see #calcTimeoutOrNone(TimeUnit, long)
     */
    public static String calcTimeoutOrYear(TimeUnit unit, long duration) {
        return unit == null || duration < 1 ? SWF_TIMEOUT_YEAR : valueOf(unit.toSeconds(duration));
    }

    /**
     * Make a unique and valid workflowId.
     * Replaces bad characters and whitespace, appends a random int, and trims to {@link #MAX_ID_LENGTH}, which also makes it easy for amazon cli use.
     *
     * @param workflowName name of workflow.
     *
     * @return unique workflowId
     */
    public static String createUniqueWorkflowId(String workflowName) {
        String name = replaceUnsafeNameChars(workflowName);
        String randomize = String.format(".%010d", ThreadLocalRandom.current().nextInt(0, Integer.MAX_VALUE));
        name = trimToMaxLength(name, MAX_ID_LENGTH - randomize.length());
        return assertSwfValue(name + randomize);
    }


    /**
     * Replace disallowed name characters and whitespace with an underscore.
     *
     * @param string string to be fixed
     *
     * @return string with replacements
     */
    public static String replaceUnsafeNameChars(String string) {
        return string.trim()
                   .replaceAll("\\s|[^\\w]", "_")
                   .replaceAll("arn", "Arn");
    }
}
