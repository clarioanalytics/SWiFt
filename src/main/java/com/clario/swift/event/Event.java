package com.clario.swift.event;

import com.amazonaws.services.simpleworkflow.model.EventType;
import com.amazonaws.services.simpleworkflow.model.HistoryEvent;
import org.joda.time.DateTime;

import static java.lang.String.format;

/**
 * @author George Coller
 */
public abstract class Event implements Comparable<Event> {

    protected final HistoryEvent historyEvent;

    protected Event(HistoryEvent historyEvent) {
        this.historyEvent = historyEvent;
    }

    public static Event create(HistoryEvent historyEvent) {
        return EventFactory.create(historyEvent);
    }

    public HistoryEvent getHistoryEvent() { return historyEvent; }

    public EventType getType() { return EventType.valueOf(historyEvent.getEventType()); }

    public Long getEventId() { return historyEvent.getEventId(); }

    public DateTime getEventTimestamp() { return new DateTime(historyEvent.getEventTimestamp()); }

    public abstract EventState getState();

    public abstract EventCategory getCategory();

    public abstract Long getInitialEventId();

    public abstract boolean isInitialAction();

    public abstract String getActionId();

    public String getInput() { throw new UnsupportedOperationException(format("%s input not available", getClass().getSimpleName())); }

    public String getOutput() { throw new UnsupportedOperationException(format("%s output not available", getClass().getSimpleName())); }

    public String getControl() { throw new UnsupportedOperationException(format("%s control not available", getClass().getSimpleName())); }

    public String getReason() { throw new UnsupportedOperationException(format("%s error reason not available", getClass().getSimpleName())); }

    public String getDetails() { throw new UnsupportedOperationException(format("%s error details not available", getClass().getSimpleName())); }

    /**
     * Two events are consider equal if they share the same {@link #getEventId()}.
     */
    public boolean equals(Object o) {
        return this == o || o instanceof Event && getEventId().equals(((Event) o).getEventId());
    }

    /**
     * @return hashCode of this event's eventId.
     */
    public int hashCode() {
        return getEventId().hashCode();
    }

    @Override
    public String toString() {
        return format("%s: %s, %s, %s, %s", getType(), getEventId(), getInitialEventId(), getActionId(), getEventTimestamp());
    }

    /**
     * Sort by eventId descending (most recent event first).
     */
    public int compareTo(Event event) {
        return -getEventId().compareTo(event.getEventId());
    }
}
