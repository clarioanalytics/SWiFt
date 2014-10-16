package com.clario.swift.event;

import com.amazonaws.services.simpleworkflow.model.*;

/**
 * @author George Coller
 */
public class ActivityTaskFailedEvent extends Event {

    protected ActivityTaskFailedEvent(HistoryEvent historyEvent) {
        super(historyEvent);
    }

    @Override public EventState getState() { return EventState.ERROR; }

    @Override public EventCategory getCategory() { return EventCategory.ACTION; }

    @Override public Long getInitialEventId() { return getScheduledEventId(); }

    @Override public boolean isInitialAction() { return false; }

    @Override public String getActionId() { return null; }

    @Override public String getReason() {  return getReason(); } 

    public ActivityTaskFailedEventAttributes getAttributes() {return historyEvent.getActivityTaskFailedEventAttributes();}

    public String getDetails() { return getAttributes().getDetails(); }

    public Long getScheduledEventId() { return getAttributes().getScheduledEventId(); }

    public Long getStartedEventId() { return getAttributes().getStartedEventId(); }

}
