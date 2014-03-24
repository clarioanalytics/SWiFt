package com.clario.swift

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow
import groovy.json.JsonOutput
import org.slf4j.Logger
import org.slf4j.LoggerFactory

// TODO: Need to make polling multi-threaded since each polling gets/executes only a single activity at a time.
/**
 * Base class for polling
 * @author George Coller
 */
public abstract class BasePoller implements Runnable {
    final Logger log
    private final String id
    AmazonSimpleWorkflow swf
    boolean isRunning = true;
    String taskList = "default"
    String domain = "domain"

    BasePoller(String id) {
        this.id = id
        log = LoggerFactory.getLogger(id)
    }

    String getId() { return id }

    void stop() {
        isRunning = false
    }

    void run() {
        log.info("Start ${toString()}")
        while (isRunning) {
            try {
                poll()
            } catch (e) {
                log.warn(this.toString(), e)
            }
        }
    }

    /**
     * Called in a loop while {@link #isRunning} is true.
     * Any exception thrown will be logged as a warning and {@link #poll} will be called again.
     */
    abstract protected void poll()


    static String makeKey(String name, String version) {
        "$name $version"
    }

    /**
     * Utility method to convert a stack trace to a String.
     */
    static String printStackTrace(Throwable t) {
        new StringWriter().withPrintWriter { t.printStackTrace(it) }.toString()
    }

    @Override
    public String toString() {
        return JsonOutput.toJson([id: id, domain: domain, taskList: taskList])
    }
}