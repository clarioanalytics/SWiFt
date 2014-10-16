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

    public ActivityTaskFailedEventAttributes getAttributes() {return historyEvent.getActivityTaskFailedEventAttributes();}

    public @Override  String getReason() { return getAttributes().getReason(); }

    public @Override  String getDetails() { return getAttributes().getDetails(); }

    public  Long getScheduledEventId() { return getAttributes().getScheduledEventId(); }

    public  Long getStartedEventId() { return getAttributes().getStartedEventId(); }

}
