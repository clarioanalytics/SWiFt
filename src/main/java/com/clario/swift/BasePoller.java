package com.clario.swift;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import groovy.json.JsonOutput;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.LinkedHashMap;

/**
 * Base class for polling
 *
 * @author George Coller
 */
public abstract class BasePoller implements Runnable {
    private final Logger log;
    private final String id;
    private AmazonSimpleWorkflow swf;
    private boolean isRunning = true;
    private String taskList = "default";
    private String domain = "domain";

    public BasePoller(String id) {
        this.id = id;
        log = LoggerFactory.getLogger(id);
    }

    public String getId() {return id;}

    public void stop() {
        isRunning = false;
    }

    public void run() {
        log.info("Start " + DefaultGroovyMethods.toString(this));
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

    public static String makeKey(String name, String version) {
        return name + " " + version;
    }

    /**
     * Utility method to convert a stack trace to a String.
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
        return JsonOutput.toJson(map);
    }

    public final Logger getLog() {
        return log;
    }

    public AmazonSimpleWorkflow getSwf() {
        return swf;
    }

    public void setSwf(AmazonSimpleWorkflow swf) { this.swf = swf; }

    public boolean isRunning() { return isRunning; }

    public void setRunning(boolean isRunning) { this.isRunning = isRunning; }

    public String getTaskList() {
        return taskList;
    }

    public void setTaskList(String taskList) {
        this.taskList = taskList;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

}
