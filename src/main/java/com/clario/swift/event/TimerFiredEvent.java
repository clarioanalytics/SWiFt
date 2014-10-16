package com.clario.swift.event;

import com.amazonaws.services.simpleworkflow.model.*;

/**
 * @author George Coller
 */
public class TimerFiredEvent extends Event {

    protected TimerFiredEvent(HistoryEvent historyEvent) {
        super(historyEvent);
    }

    @Override public EventState getState() { return EventState.SUCCESS; }

    @Override public EventCategory getCategory() { return EventCategory.ACTION; }

    @Override public Long getInitialEventId() { return getStartedEventId(); }

    @Override public boolean isInitialAction() { return false; }

    @Override public String getActionId() { return getTimerId(); }

    @Override public String getOutput() {  return null; } 

    public TimerFiredEventAttributes getAttributes() {return historyEvent.getTimerFiredEventAttributes();}

    public String getTimerId() { return getAttributes().getTimerId(); }

    public Long getStartedEventId() { return getAttributes().getStartedEventId(); }

}
