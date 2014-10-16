package com.clario.swift.event;

import com.amazonaws.services.simpleworkflow.model.*;

/**
 * @author George Coller
 */
public class ActivityTaskCancelRequestedEvent extends Event {

    protected ActivityTaskCancelRequestedEvent(HistoryEvent historyEvent) {
        super(historyEvent);
    }

    @Override public EventState getState() { return EventState.INFO; }

    @Override public EventCategory getCategory() { return EventCategory.EXTERNAL; }

    @Override public Long getInitialEventId() { return getEventId(); }

    @Override public boolean isInitialAction() { return false; }

    @Override public String getActionId() { return null; }

    public ActivityTaskCancelRequestedEventAttributes getAttributes() {return historyEvent.getActivityTaskCancelRequestedEventAttributes();}

    public  Long getDecisionTaskCompletedEventId() { return getAttributes().getDecisionTaskCompletedEventId(); }

    public  String getActivityId() { return getAttributes().getActivityId(); }

}
