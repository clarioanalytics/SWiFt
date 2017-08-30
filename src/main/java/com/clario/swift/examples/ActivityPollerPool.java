package com.clario.swift.examples;

import com.clario.swift.ActivityPoller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.clario.swift.examples.Config.config;
import static java.lang.String.format;

/**
 * Example demonstrating creating a thread pool of {@link ActivityPoller} instances.
 *
 * @author George Coller
 */
public class ActivityPollerPool {

    private static final Logger log = LoggerFactory.getLogger(DecisionPollerPool.class);

    public static void main(String[] args) throws IOException, InterruptedException {
        final ScheduledExecutorService service = Executors.newScheduledThreadPool(config().getActivityPoolSize());

        for (int it = 1; it <= config().getActivityPoolSize(); it++) {
            ActivityPoller poller = new ActivityPoller(format("activity poller %s", it), config().getDomain(), config().getTaskList());
            poller.setSwf(config().getSWF());
            poller.addActivities(new ActivityMethods());
            if (config().isRegisterActivities() && it == 1) {
                poller.registerSwfActivities();
            }
            log.info(format("start: %s domain=%s taskList=%s", poller.getId(), config().getDomain(), config().getTaskList()));
            service.scheduleWithFixedDelay(poller, 1, 1, TimeUnit.SECONDS);
        }

        config().registerShutdownMethod(service);
        log.info("activity pollers started:");
    }
}
