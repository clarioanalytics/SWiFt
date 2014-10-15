package com.clario.swift;

import com.clario.swift.event.Event;

/**
 * Interface provided for writing selection functions that can be used with {@link EventList#select}.
 *
 * @author George Coller
 */
public interface SelectFunction {

    /**
     * @param event actionEvent to test for select or ignore.
     * @param index index of event in list
     * @param eventList event list being selected against
     *
     * @return true if item should be selected, otherwise false.
     */
    boolean select(Event event, int index, EventList eventList);
}
