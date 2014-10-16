package com.clario.swift.event;

import com.amazonaws.services.simpleworkflow.model.*;

/**
 * @author George Coller
 */
public class DecisionTaskScheduledEvent extends Event {

    protected DecisionTaskScheduledEvent(HistoryEvent historyEvent) {
        super(historyEvent);
    }

    @Override public EventState getState() { return EventState.INFO; }

    @Override public EventCategory getCategory() { return EventCategory.EXTERNAL; }

    @Override public Long getInitialEventId() { return getEventId(); }

    @Override public boolean isInitialAction() { return false; }

    @Override public String getActionId() { return null; }


    public DecisionTaskScheduledEventAttributes getAttributes() {return historyEvent.getDecisionTaskScheduledEventAttributes();}

    public String getTaskList() { return getAttributes().getTaskList().getName(); }

    public String getStartToCloseTimeout() { return getAttributes().getStartToCloseTimeout(); }

}
