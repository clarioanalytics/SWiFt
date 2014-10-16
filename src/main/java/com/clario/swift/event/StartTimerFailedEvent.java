package com.clario.swift.event;

import com.amazonaws.services.simpleworkflow.model.*;

/**
 * @author George Coller
 */
public class StartTimerFailedEvent extends Event {

    protected StartTimerFailedEvent(HistoryEvent historyEvent) {
        super(historyEvent);
    }

    @Override public EventState getState() { return EventState.ERROR; }

    @Override public EventCategory getCategory() { return EventCategory.ACTION; }

    @Override public Long getInitialEventId() { return null; }

    @Override public boolean isInitialAction() { return false; }

    @Override public String getActionId() { return getTimerId(); }

    @Override public String getReason() {  return null; } 

    public StartTimerFailedEventAttributes getAttributes() {return historyEvent.getStartTimerFailedEventAttributes();}

    public String getTimerId() { return getAttributes().getTimerId(); }

    public String getCause() { return getAttributes().getCause(); }

    public Long getDecisionTaskCompletedEventId() { return getAttributes().getDecisionTaskCompletedEventId(); }

}
