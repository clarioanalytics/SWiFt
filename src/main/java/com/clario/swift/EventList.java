package com.clario.swift;

import com.amazonaws.services.simpleworkflow.model.EventType;
import com.amazonaws.services.simpleworkflow.model.HistoryEvent;
import com.clario.swift.action.Action;
import com.clario.swift.action.RetryPolicy;

import java.util.*;

import static com.amazonaws.services.simpleworkflow.model.EventType.DecisionTaskCompleted;
import static com.amazonaws.services.simpleworkflow.model.EventType.TimerStarted;
import static java.lang.String.format;
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

    @Override
    public int size() { return eventList.size(); }

    /**
     * @return true if this list has one or more items.
     */
    public boolean isNotEmpty() { return !isEmpty(); }

    /**
     * @return first item in this list or null if list is empty.
     */
    public Event getFirst() {
        return eventList.isEmpty() ? null : eventList.get(0);
    }

    /**
     * @return last item in this list or null if list is empty.
     */
    public Event getLast() {
        return eventList.isEmpty() ? null : eventList.get(eventList.size() - 1);
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

    public EventList selectEventState(Event.State state) {return select(byEventState(state));}

    public EventList selectSinceLastDecision() {return select(bySinceLastDecision());}

    /**
     * @param control optional, limit to a single policy's {@link RetryPolicy#getControl()} value.
     *
     * @return number of times a given {@link RetryPolicy}s timer has been started.
     */
    public EventList selectRetryCount(final String control) {
        return select(new SelectFunction() {
            public boolean select(Event event, int index, EventList eventList) {
                return TimerStarted == event.getType() && control.equals(event.getData1());
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
                        if (ev.isInitialAction() && ev.getActionId().equals(actionId)) {
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
     * Select events by {@link Event.State}.
     */
    public static SelectFunction byEventState(final Event.State state) {
        return new SelectFunction() {
            public boolean select(Event event, int index, EventList eventList) {
                return event.getActionState() == state;
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

    /**
     * Select events where all field values are equal to the ones in a provided field/value map.
     * <p/>
     * Note: null is allowed as a value in the map and will match null event fields.
     *
     * @param map of {@link Event.Field} / value. See {@link #createFieldMap} for an easy way to create this map.
     */
    public static SelectFunction byEventFieldsEquals(final Map<Event.Field, Object> map) {
        return new SelectFunction() {
            public boolean select(Event event, int index, EventList eventList) {
                for (Map.Entry<Event.Field, Object> entry : map.entrySet()) {
                    Object actionEventValue = event.getField(entry.getKey(), false);
                    Object entryValue = entry.getValue();
                    if (entryValue == null ? actionEventValue != null : !entryValue.equals(actionEventValue)) {
                        return false;
                    }
                }
                return true;
            }
        };
    }

    /**
     * Convenience method for creating {@link Event.Field} / value maps.
     * <pre>
     * Example usage:
     * <code>
     * Map map = createFieldMap(Event.Field1, Value1, Event.Field2, Value2, ...)
     * </code></pre>
     *
     * @see #byEventFieldsEquals(Map)
     */
    public static Map<Event.Field, Object> createFieldMap(Event.Field field, Object value, Object... fieldObject) {
        Map<Event.Field, Object> map = new HashMap<Event.Field, Object>();
        map.put(field, value);
        if (fieldObject.length > 0) {
            if (fieldObject.length % 2 > 0) {
                throw new IllegalArgumentException("Even number of fieldObject values required");
            }
            for (int i = 0; i < fieldObject.length; i += 2) {
                Object f = fieldObject[i];
                if (f == null || !f.getClass().equals(Event.Field.class)) {
                    throw new IllegalArgumentException(format("Parameter fieldObject[%d]=%s is not expected type %s", i, f, Event.Field.class.getSimpleName()));
                }
                map.put((Event.Field) f, fieldObject[i + 1]);
            }
        }
        return map;
    }
}
