package com.clario.swift.event;

import com.amazonaws.services.simpleworkflow.model.*;

/**
 * @author George Coller
 */
public class ActivityTaskScheduledEvent extends Event {

    protected ActivityTaskScheduledEvent(HistoryEvent historyEvent) {
        super(historyEvent);
    }

    @Override public EventState getState() { return EventState.ACTIVE; }

    @Override public EventCategory getCategory() { return EventCategory.ACTION; }

    @Override public Long getInitialEventId() { return getEventId(); }

    @Override public boolean isInitialAction() { return true; }

    @Override public String getActionId() { return getActivityId(); }

    @Override public String getData1() { return getInput(); }

    @Override public String getData2() { return getControl(); }

    public ActivityTaskScheduledEventAttributes getAttributes() {return historyEvent.getActivityTaskScheduledEventAttributes();}

    public String getActivityTypeName() { return getAttributes().getActivityType().getName(); }

    public String getActivityTypeVersion() { return getAttributes().getActivityType().getVersion(); }

    public String getActivityId() { return getAttributes().getActivityId(); }

    public String getInput() { return getAttributes().getInput(); }

    public String getControl() { return getAttributes().getControl(); }

    public String getScheduleToStartTimeout() { return getAttributes().getScheduleToStartTimeout(); }

    public String getScheduleToCloseTimeout() { return getAttributes().getScheduleToCloseTimeout(); }

    public String getStartToCloseTimeout() { return getAttributes().getStartToCloseTimeout(); }

    public String getTaskList() { return getAttributes().getTaskList().getName(); }

    public Long getDecisionTaskCompletedEventId() { return getAttributes().getDecisionTaskCompletedEventId(); }

    public String getHeartbeatTimeout() { return getAttributes().getHeartbeatTimeout(); }

}
