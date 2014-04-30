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
    protected final Logger log;
    private final String id;
    protected final String taskList;
    protected final String domain;
    protected AmazonSimpleWorkflow swf;

    /**
     * @param id unique id for poller, used for logging and recording in SWF
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
     * @see #poll() actual polling method called within this loop
     */
    public void run() {
        poll();
    }

    /**
     * Subclass implements to perform the SWF polling work.
     *
     * @see #run for scheduling this poller in a thread pool
     */
    protected abstract void poll();

    @Override
    public String toString() {
        LinkedHashMap<String, String> map = new LinkedHashMap<>(3);
        map.put("id", id);
        map.put("domain", domain);
        map.put("taskList", taskList);
        return toJson(map);
    }

    public void setSwf(AmazonSimpleWorkflow swf) { this.swf = swf; }
}
