package com.clario.swift.event;

import com.amazonaws.services.simpleworkflow.model.*;

/**
 * @author George Coller
 */
public class DecisionTaskCompletedEvent extends Event {

    protected DecisionTaskCompletedEvent(HistoryEvent historyEvent) {
        super(historyEvent);
    }

    @Override public EventState getState() { return EventState.SUCCESS; }

    @Override public EventCategory getCategory() { return EventCategory.DECISION; }

    @Override public Long getInitialEventId() { return getScheduledEventId(); }

    @Override public boolean isInitialAction() { return false; }

    @Override public String getActionId() { return null; }

    @Override public String getData1() { return getExecutionContext(); }

    @Override public String getData2() { return null; }

    public DecisionTaskCompletedEventAttributes getAttributes() {return historyEvent.getDecisionTaskCompletedEventAttributes();}

    public String getExecutionContext() { return getAttributes().getExecutionContext(); }

    public Long getScheduledEventId() { return getAttributes().getScheduledEventId(); }

    public Long getStartedEventId() { return getAttributes().getStartedEventId(); }

}
