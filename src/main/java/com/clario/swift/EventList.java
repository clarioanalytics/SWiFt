package com.clario.swift;

import com.amazonaws.services.simpleworkflow.model.EventType;
import com.amazonaws.services.simpleworkflow.model.HistoryEvent;
import com.clario.swift.action.Action;
import com.clario.swift.action.RetryPolicy;
import com.clario.swift.event.Event;
import com.clario.swift.event.EventState;

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.amazonaws.services.simpleworkflow.model.EventType.DecisionTaskCompleted;
import static com.amazonaws.services.simpleworkflow.model.EventType.TimerStarted;
import static com.clario.swift.event.EventState.INITIAL;
import static java.util.Arrays.copyOfRange;

/**
 * {@link List} implementation of {@link Event} with convenient selection methods and functions.
 * <p/>
 * Note: Intended to be immutable, calling any method that of {@link List} that modifies the list will throw {@link UnsupportedOperationException}
 *
 * @author George Coller
 */
public class EventList extends AbstractList<Event> {
    private final List<Event> eventList;

    public EventList() { eventList = Collections.emptyList(); }

    public EventList(List<Event> actionEvents) {
        this.eventList = actionEvents;
    }

    /**
     * Convert a list of SWF {@link HistoryEvent} to an {@link EventList} of {@link Event}.
     */
    public static EventList convert(List<HistoryEvent> historyEvents) {
        List<Event> actionEvents = new ArrayList<Event>(historyEvents.size());
        for (HistoryEvent historyEvent : historyEvents) {
            actionEvents.add(new Event(historyEvent));
        }
        return new EventList(actionEvents);
    }

    @Override
    public Event get(int index) { return eventList.get(index); }

    @SuppressWarnings("unchecked")
    public <T extends Event> T getAs(int index) { return (T) get(index); }

    @Override
    public int size() { return eventList.size(); }

    /**
     * @return true if this list has one or more items.
     */
    public boolean isNotEmpty() { return !isEmpty(); }

    /**
     * @return first item in this list or null if list is empty.
     */
    @SuppressWarnings("unchecked")
    public <T extends Event> T getFirst() {
        return eventList.isEmpty() ? null : (T) eventList.get(0);
    }

    /**
     * @return last item in this list or null if list is empty.
     */
    @SuppressWarnings("unchecked")
    public <T extends Event> T getLast() {
        return eventList.isEmpty() ? null : (T) eventList.get(eventList.size() - 1);
    }

    /**
     * Sort list by {@link Event} {@link Comparable} implementation, which is by {@link Event#getEventId()} in descending order.
     */
    public void sort() {
        Collections.sort(eventList);
    }

    /**
     * Select {@link Event} by zero or more {@link SelectFunction} instances where the output of one
     * select is the input to the next
     *
     * @param selectFunctions list of select functions to apply.
     *
     * @return new {@link EventList}
     */
    public EventList select(SelectFunction... selectFunctions) {
        return select(this, selectFunctions);
    }

    /**
     * Select recursively where the output of applying one {@link SelectFunction} is passed in as
     * the {@link EventList} of the next.
     *
     * @param eventList list to select against
     * @param selectFunctions ordered list of select functions to apply.
     *
     * @return new {@link EventList}
     */
    public static EventList select(EventList eventList, SelectFunction... selectFunctions) {
        if (selectFunctions.length > 0) {
            List<Event> selected = new ArrayList<Event>();
            for (int i = 0; i < eventList.size(); i++) {
                Event event = eventList.get(i);
                if (selectFunctions[0].select(event, i, eventList)) {
                    selected.add(event);
                }
            }
            return select(new EventList(selected), copyOfRange(selectFunctions, 1, selectFunctions.length));
        } else {
            return eventList;
        }
    }

    // Specific methods for common selections
    public EventList selectActionId(String actionId) {return select(byActionId(actionId));}

    public EventList selectEventType(EventType eventType) {return select(byEventType(eventType));}

    public EventList selectTaskType(TaskType taskType) { return select(byTaskType(taskType)); }

    public EventList selectEventState(EventState eventState) {return select(byEventState(eventState));}

    public EventList selectSinceLastDecision() {return select(bySinceLastDecision());}

    /**
     * @param control optional, limit to a single policy's {@link RetryPolicy#getControl()} value.
     *
     * @return number of times a given {@link RetryPolicy}s timer has been started.
     */
    public EventList selectRetryCount(final String control) {
        return select(new SelectFunction() {
            public boolean select(Event event, int index, EventList eventList) {
                return TimerStarted == event.getType() && control.equals(event.getHistoryEvent().getTimerStartedEventAttributes().getControl());
            }
        });
    }

    /**
     * Select events related to an {@link Action}.
     *
     * @param actionId action identifier of the action
     *
     * @return new {@link EventList}
     */
    public static SelectFunction byActionId(final String actionId) {
        return new SelectFunction() {
            List<Long> initialEventIds = new ArrayList<Long>();

            public boolean select(Event event, int index, EventList eventList) {
                if (index == 0) {
                    for (Event ev : eventList) {
                        if (INITIAL == ev.getState() && actionId.equals(ev.getActionId())) {
                            initialEventIds.add(ev.getEventId());
                        }
                    }
                }
                return initialEventIds.contains(event.getEventId()) || initialEventIds.contains(event.getInitialEventId());
            }
        };
    }

    /**
     * Select events by {@link EventType}.
     */
    public static SelectFunction byEventType(final EventType eventType) {
        return new SelectFunction() {
            public boolean select(Event event, int index, EventList eventList) {
                return event.getType() == eventType;
            }
        };
    }

    /**
     * Select events by {@link TaskType}.
     */
    public static SelectFunction byTaskType(final TaskType taskType) {
        return new SelectFunction() {
            public boolean select(Event event, int index, EventList eventList) {
                return event.getTask() == taskType;
            }
        };
    }

    /**
     * Select events by {@link EventState}.
     */
    public static SelectFunction byEventState(final EventState eventState) {
        return new SelectFunction() {
            public boolean select(Event event, int index, EventList eventList) {
                return event.getState() == eventState;
            }
        };
    }

    /**
     * Select events whose {@link Event#getEventId} is greater than the most recent
     * {@link EventType#DecisionTaskCompleted} event.
     */
    public static SelectFunction bySinceLastDecision() {
        return new SelectFunction() {
            Long eventId = -1L;

            public boolean select(Event event, int index, EventList eventList) {
                if (index == 0) {
                    for (Event e : eventList) {
                        if (DecisionTaskCompleted == e.getType()) {
                            eventId = e.getEventId();
                            break;
                        }
                    }
                }
                return event.getEventId() > eventId;
            }
        };
    }

    /**
     * Select events within a given {@link Event#getEventId()} range.
     *
     * @param startEventId minimum event id in range.
     * @param endEventId maximum eventId in range.
     */
    public static SelectFunction byEventIdRange(final long startEventId, final long endEventId) {
        return new SelectFunction() {
            public boolean select(Event event, int index, EventList eventList) {
                return event.getEventId() >= startEventId && event.getEventId() <= endEventId;
            }
        };
    }
}
