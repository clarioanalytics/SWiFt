package com.clario.swift.event;

import com.amazonaws.services.simpleworkflow.model.HistoryEvent;
import com.amazonaws.services.simpleworkflow.model.MarkerRecordedEventAttributes;

/**
 * @author George Coller
 */
public class MarkerRecordedEvent extends Event {

    protected MarkerRecordedEvent(HistoryEvent historyEvent) {
        super(historyEvent);
    }

    @Override public EventState getState() { return EventState.SUCCESS; }

    @Override public EventCategory getCategory() { return EventCategory.ACTION; }

    @Override public Long getInitialEventId() { return getEventId(); }

    @Override public boolean isInitialAction() { return true; }

    @Override public String getActionId() { return getMarkerName(); }

    @Override public String getOutput() { return getDetails(); }

    public MarkerRecordedEventAttributes getAttributes() {return historyEvent.getMarkerRecordedEventAttributes();}

    public String getMarkerName() { return getAttributes().getMarkerName(); }

    public @Override String getDetails() { return getAttributes().getDetails(); }

    public Long getDecisionTaskCompletedEventId() { return getAttributes().getDecisionTaskCompletedEventId(); }

}
