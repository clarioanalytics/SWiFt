package com.clario.swift.event;

import com.amazonaws.services.simpleworkflow.model.*;

/**
 * @author George Coller
 */
public class ActivityTaskCompletedEvent extends Event {

    protected ActivityTaskCompletedEvent(HistoryEvent historyEvent) {
        super(historyEvent);
    }

    @Override public EventState getState() { return EventState.SUCCESS; }

    @Override public EventCategory getCategory() { return EventCategory.ACTION; }

    @Override public Long getInitialEventId() { return getScheduledEventId(); }

    @Override public boolean isInitialAction() { return false; }

    @Override public String getActionId() { return null; }

    @Override public String getData1() { return getResult(); }

    @Override public String getData2() { return null; }

    public ActivityTaskCompletedEventAttributes getAttributes() {return historyEvent.getActivityTaskCompletedEventAttributes();}

    public String getResult() { return getAttributes().getResult(); }

    public Long getScheduledEventId() { return getAttributes().getScheduledEventId(); }

    public Long getStartedEventId() { return getAttributes().getStartedEventId(); }

}
