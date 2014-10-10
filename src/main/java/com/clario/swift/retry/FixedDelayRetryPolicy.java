package com.clario.swift.retry;

import com.clario.swift.EventList;

/**
 * @author George Coller
 */
public class FixedDelayRetryPolicy extends RetryPolicy {

    private int delayTimeInSeconds;

    public FixedDelayRetryPolicy(int delayTimeInSeconds) { this.delayTimeInSeconds = delayTimeInSeconds; }

    @Override int nextRetryDelaySeconds(EventList events) {
        return delayTimeInSeconds;
    }

    @Override public void validate() {
        // do nothing
    }

}
