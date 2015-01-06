package com.clario.swift;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Base class for Activity and Decision pollers.
 *
 * @author George Coller
 */
public abstract class BasePoller implements Runnable {
    protected final Logger log;
    private final String id;
    protected final String taskList;
    protected final String domain;
    protected AmazonSimpleWorkflow swf;
    private int logHeartbeatMinutes = 10;
    private long priorHeartbeatTime = System.currentTimeMillis();

    /**
     * @param id unique id for poller used for logging and recording in SWF
     * @param domain SWF domain to poll
     * @param taskList SWF taskList to filter on
     */
    public BasePoller(String id, String domain, String taskList) {
        this.id = id;
        this.domain = domain;
        this.taskList = taskList;
        log = LoggerFactory.getLogger(id);
    }

    public String getId() {return id;}

    /**
     * {@link Runnable#run} implementation calls {@link #poll()} once,
     * allows for scheduling multiple poller instances in an external thread pool.
     *
     * @see #poll
     */
    public void run() {
        log.debug("run");
        try {
            poll();
        } catch (Throwable t) {
            log.error(this.toString(), t);
        }
    }

    /**
     * Subclass implements to perform the SWF polling work.
     *
     * @see #run
     */
    protected abstract void poll();

    /**
     * Avoid filling the log with 'timeout' messages by recording it every {@link #setLogHeartbeatMinutes}.
     * Default is 10 minutes.
     */
    protected boolean isLogTimeout() {
        DateTime prior = new DateTime(priorHeartbeatTime);
        DateTime now = DateTime.now();
        if (new Duration(prior, now).getStandardMinutes() >= logHeartbeatMinutes) {
            priorHeartbeatTime = System.currentTimeMillis();
            return true;
        }
        return false;
    }

    /**
     * Set how often a poller logs 'timeout' messages.
     *
     * @param minutes must be a positive integer
     *
     * @see #isLogTimeout()
     */
    public void setLogHeartbeatMinutes(int minutes) {
        if (minutes < 1) { throw new IllegalArgumentException("parameter minutes must be greater than zero"); }
        this.logHeartbeatMinutes = minutes;
    }

    /**
     * Two pollers are equal if they have the same {@link #id} value.
     */
    @Override
    public boolean equals(Object o) {
        return o == this || (o != null && o instanceof BasePoller && id.equals(((BasePoller) o).id));
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return String.format("Poller '%s': %s %s", id, domain, taskList);
    }

    public void setSwf(AmazonSimpleWorkflow swf) { this.swf = swf; }
}
