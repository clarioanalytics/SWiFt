package com.clario.swift.event;

import com.amazonaws.services.simpleworkflow.model.*;

/**
 * @author George Coller
 */
public class ActivityTaskStartedEvent extends Event {

    protected ActivityTaskStartedEvent(HistoryEvent historyEvent) {
        super(historyEvent);
    }

    @Override public EventState getState() { return EventState.ACTIVE; }

    @Override public EventCategory getCategory() { return EventCategory.ACTION; }

    @Override public Long getInitialEventId() { return getScheduledEventId(); }

    @Override public boolean isInitialAction() { return false; }

    @Override public String getActionId() { return null; }

    public ActivityTaskStartedEventAttributes getAttributes() {return historyEvent.getActivityTaskStartedEventAttributes();}

    public  String getIdentity() { return getAttributes().getIdentity(); }

    public  Long getScheduledEventId() { return getAttributes().getScheduledEventId(); }

}
