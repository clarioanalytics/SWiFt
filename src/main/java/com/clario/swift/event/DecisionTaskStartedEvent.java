package com.clario.swift.event;

import com.amazonaws.services.simpleworkflow.model.*;

/**
 * @author George Coller
 */
public class DecisionTaskStartedEvent extends Event {

    protected DecisionTaskStartedEvent(HistoryEvent historyEvent) {
        super(historyEvent);
    }

    @Override public EventState getState() { return EventState.INFO; }

    @Override public EventCategory getCategory() { return EventCategory.EXTERNAL; }

    @Override public Long getInitialEventId() { return getEventId(); }

    @Override public boolean isInitialAction() { return false; }

    @Override public String getActionId() { return null; }


    public DecisionTaskStartedEventAttributes getAttributes() {return historyEvent.getDecisionTaskStartedEventAttributes();}

    public String getIdentity() { return getAttributes().getIdentity(); }

    public Long getScheduledEventId() { return getAttributes().getScheduledEventId(); }

}
