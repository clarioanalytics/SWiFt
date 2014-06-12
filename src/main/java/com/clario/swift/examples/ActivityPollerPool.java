package com.clario.swift.examples;

import com.clario.swift.ActivityContext;
import com.clario.swift.ActivityMethod;
import com.clario.swift.ActivityPoller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.clario.swift.examples.Config.config;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Launch a couple of activity pollers with activities used in the examples.
 * @author George Coller
 */
public class ActivityPollerPool {

    private static final Logger log = LoggerFactory.getLogger(DecisionPollerPool.class);

    public static void main(String[] args) throws IOException, InterruptedException {
        final ScheduledExecutorService service = Executors.newScheduledThreadPool(config.getActivityPoolSize());

        for (int it = 1; it <= config.getActivityPoolSize(); it++) {
            ActivityPoller poller = new ActivityPoller(format("activity poller %s", it), config.getDomain(), config.getTaskList());
            poller.setSwf(config.getSWF());
            poller.addActivities(new ActivityPollerPool());
            if (config.isRegisterActivities() && it == 1) {
                poller.registerSwfActivities();
            }
            log.info(format("start: %s domain=%s taskList=%s", poller.getId(), config.getDomain(), config.getTaskList()));
            service.scheduleWithFixedDelay(poller, 1, 1, TimeUnit.SECONDS);
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                System.out.println("Shutting Down Simple Workflow Service");
                config.getSWF().shutdown();
                System.out.println("Shutting Down Pool");
                service.shutdownNow();
            }
        });
    }

    //---------------------------------------
    // Example ActivityMethod impls
    //---------------------------------------

    @ActivityMethod(name = "Activity Fail Until", version = "1.0", description = "input a time in UTC and this activity will fail if called before it")
    public void failUntilTime(ActivityContext context) {
        long failIfBeforeThisTime = Long.parseLong(context.getInput());
        long currentTime = System.currentTimeMillis();
        if (currentTime < failIfBeforeThisTime) {
            throw new IllegalStateException("Still too early: " + MILLISECONDS.toSeconds(failIfBeforeThisTime - currentTime) + " seconds left");
        }
    }

    @ActivityMethod(name = "Activity Echo", version = "1.0", description = "log and return input",
        scheduleToCloseTimeout = "NONE", scheduleToStartTimeout = "NONE", startToCloseTimeout = "60")
    public String activityEcho(ActivityContext context) {
        String input = context.getInput();
        log.info(input);
        return input;
    }

    @ActivityMethod(name = "Activity X", version = "1.0")
    public Integer activityX(ActivityContext context) {
        final int i = Integer.parseInt(context.getInput());
        sleep(SECONDS.toMillis(2));
        return i + 1;
    }

    @ActivityMethod(name = "Activity Y", version = "1.0")
    public Integer activityY(ActivityContext context) {
        final int i = Integer.parseInt(context.getInput());
        sleep(SECONDS.toMillis(2));
        return i + 100;
    }

    @ActivityMethod(name = "Activity Z", version = "1.0")
    public Integer activityZ(final ActivityContext context) {
        final int i = Integer.parseInt(context.getInput());
        sleep(SECONDS.toMillis(5));
        log.info("Activity Z " + context.getActionId() + ": record heartbeat");
        context.recordHeartbeat("some deets: " + valueOf(new Date()));
        sleep(SECONDS.toMillis(10));
        return i + 1000;
    }

    private static void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            throw new IllegalStateException("Interrupted", e);
        }
    }
}
