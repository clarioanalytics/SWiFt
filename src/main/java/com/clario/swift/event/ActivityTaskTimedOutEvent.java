package com.clario.swift.event;

import com.amazonaws.services.simpleworkflow.model.*;

/**
 * @author George Coller
 */
public class ActivityTaskTimedOutEvent extends Event {

    protected ActivityTaskTimedOutEvent(HistoryEvent historyEvent) {
        super(historyEvent);
    }

    @Override public EventState getState() { return EventState.ERROR; }

    @Override public EventCategory getCategory() { return EventCategory.ACTION; }

    @Override public Long getInitialEventId() { return getScheduledEventId(); }

    @Override public boolean isInitialAction() { return false; }

    @Override public String getActionId() { return null; }

    @Override public String getReason() {  return getTimeoutType(); }

    public ActivityTaskTimedOutEventAttributes getAttributes() {return historyEvent.getActivityTaskTimedOutEventAttributes();}

    public  String getTimeoutType() { return getAttributes().getTimeoutType(); }

    public  Long getScheduledEventId() { return getAttributes().getScheduledEventId(); }

    public  Long getStartedEventId() { return getAttributes().getStartedEventId(); }

    public @Override  String getDetails() { return getAttributes().getDetails(); }

}
