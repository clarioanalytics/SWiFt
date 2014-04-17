package com.clario.swift;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedHashMap;

import static com.clario.swift.SwiftUtil.toJson;

/**
 * Base class for Activity and Decision pollers.
 *
 * @author George Coller
 */
public abstract class BasePoller implements Runnable {
    protected final Logger log;
    protected final String id;
    protected final String taskList;
    protected final String domain;
    protected AmazonSimpleWorkflow swf;
    private boolean isRunning = true;

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
     * Call to gracefully stop the poller after it is finished with the current polling.
     */
    public void stop() {
        isRunning = false;
    }

    /**
     * Start polling in an endless loop until this thread is interrupted or stop is called.
     * Exceptions thrown during loop will be logged.
     *
     * @see #stop() method to stop the run
     * @see #poll() actual polling method called within this loop
     */
    public void run() {
        log.info("Start " + this);
        while (isRunning) {
            try {
                poll();
            } catch (Throwable e) {
                log.warn(this.toString(), e);
            }
        }
    }

    /**
     * Called in a loop while {@link #isRunning} is true.
     * Any exception thrown will be logged as a warning and <code>poll</code> will be called again.
     */
    protected abstract void poll();

    /**
     * Combine a name and version into a single string for easier indexing in maps, etc.
     * In SWF registered workflows and activities are identified by the combination of name and version.
     */
    public static String makeKey(String name, String version) {
        return name + " " + version;
    }

    /**
     * Utility method to convert a stack trace to a String
     */
    public static String printStackTrace(final Throwable t) {
        StringWriter sw = new StringWriter();
        t.printStackTrace(new PrintWriter(sw));
        return sw.toString();
    }

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
