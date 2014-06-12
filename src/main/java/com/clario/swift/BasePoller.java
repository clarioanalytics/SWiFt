package com.clario.swift;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;

import static com.clario.swift.SwiftUtil.toJson;

/**
 * Base class for Activity and Decision pollers.
 *
 * @author George Coller
 */
public abstract class BasePoller implements Runnable {
    private static final int LOG_POLL_EVERY_COUNT = 10;
    protected final Logger log;
    private final String id;
    protected final String taskList;
    protected final String domain;
    protected AmazonSimpleWorkflow swf;
    private int pollCount;

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
     * Avoid filling the log with 'timeout' messages by recording it every tenth time.
     */
    protected boolean isLogTimeout() {
        pollCount++;
        return LOG_POLL_EVERY_COUNT % pollCount == 0;
    }

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
        LinkedHashMap<String, String> map = new LinkedHashMap<String, String>(3);
        map.put("id", id);
        map.put("domain", domain);
        map.put("taskList", taskList);
        return toJson(map);
    }

    public void setSwf(AmazonSimpleWorkflow swf) { this.swf = swf; }
}
