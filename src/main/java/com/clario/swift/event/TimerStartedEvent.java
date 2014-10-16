package com.clario.swift.event;

import com.amazonaws.services.simpleworkflow.model.*;

/**
 * @author George Coller
 */
public class TimerStartedEvent extends Event {

    protected TimerStartedEvent(HistoryEvent historyEvent) {
        super(historyEvent);
    }

    @Override public EventState getState() { return EventState.ACTIVE; }

    @Override public EventCategory getCategory() { return EventCategory.ACTION; }

    @Override public Long getInitialEventId() { return getEventId(); }

    @Override public boolean isInitialAction() { return true; }

    @Override public String getActionId() { return getTimerId(); }

    @Override public String getInput() {  return null; } 
    @Override public String getControl() {  return getControl(); } 

    public TimerStartedEventAttributes getAttributes() {return historyEvent.getTimerStartedEventAttributes();}

    public String getTimerId() { return getAttributes().getTimerId(); }

    public String getStartToFireTimeout() { return getAttributes().getStartToFireTimeout(); }

    public Long getDecisionTaskCompletedEventId() { return getAttributes().getDecisionTaskCompletedEventId(); }

}
