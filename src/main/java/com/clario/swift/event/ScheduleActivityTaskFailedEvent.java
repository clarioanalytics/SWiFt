package com.clario.swift.event;

import com.amazonaws.services.simpleworkflow.model.*;

/**
 * @author George Coller
 */
public class ScheduleActivityTaskFailedEvent extends Event {

    protected ScheduleActivityTaskFailedEvent(HistoryEvent historyEvent) {
        super(historyEvent);
    }

    @Override public EventState getState() { return EventState.ERROR; }

    @Override public EventCategory getCategory() { return EventCategory.ACTION; }

    @Override public Long getInitialEventId() { return getEventId(); }

    @Override public boolean isInitialAction() { return false; }

    @Override public String getActionId() { return null; }

    @Override public String getReason() {  return "ScheduleActivityTaskFailed"; }

    @Override public String getDetails() {  return getCause(); }

    public ScheduleActivityTaskFailedEventAttributes getAttributes() {return historyEvent.getScheduleActivityTaskFailedEventAttributes();}

    public  String getActivityTypeName() { return getAttributes().getActivityType().getName(); }

    public  String getActivityTypeVersion() { return getAttributes().getActivityType().getVersion(); }

    public  String getActivityId() { return getAttributes().getActivityId(); }

    public  String getCause() { return getAttributes().getCause(); }

    public  Long getDecisionTaskCompletedEventId() { return getAttributes().getDecisionTaskCompletedEventId(); }

}