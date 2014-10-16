package com.clario.swift.event;

import com.amazonaws.services.simpleworkflow.model.*;

/**
 * @author George Coller
 */
public class RecordMarkerFailedEvent extends Event {

    protected RecordMarkerFailedEvent(HistoryEvent historyEvent) {
        super(historyEvent);
    }

    @Override public EventState getState() { return EventState.ERROR; }

    @Override public EventCategory getCategory() { return EventCategory.ACTION; }

    @Override public Long getInitialEventId() { return getEventId(); }

    @Override public boolean isInitialAction() { return false; }

    @Override public String getActionId() { return null; }

    @Override public String getReason() {  return "RecordMarkerFailed"; }

    @Override public String getDetails() {  return getCause(); }

    public RecordMarkerFailedEventAttributes getAttributes() {return historyEvent.getRecordMarkerFailedEventAttributes();}

    public  String getMarkerName() { return getAttributes().getMarkerName(); }

    public  String getCause() { return getAttributes().getCause(); }

    public  Long getDecisionTaskCompletedEventId() { return getAttributes().getDecisionTaskCompletedEventId(); }

}
