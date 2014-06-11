package com.clario.swift.examples;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
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

import static com.clario.swift.examples.Config.*;
import static java.lang.String.valueOf;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Launch a couple of activity pollers with activities used in the examples.
 * @author George Coller
 */
public class StartActivityPollers {

    private static final Logger log = LoggerFactory.getLogger(StartDecisionPollers.class.getSimpleName());

    public static void main(String[] args) throws IOException, InterruptedException {
        final Config config = getConfig();
        final AmazonSimpleWorkflow swf = config.getSWF();
        final ScheduledExecutorService service = Executors.newScheduledThreadPool(config.getPoolSize());
        startActivityPoller(service, swf, config.getPoolSize(), config.isRegisterActivities());

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                System.out.println("Shutting Down Simple Workflow Service");
                swf.shutdown();
                System.out.println("Shutting Down Pool");
                service.shutdownNow();
            }
        });
    }

    protected static void startActivityPoller(ScheduledExecutorService pool, AmazonSimpleWorkflow swf, int threads, boolean registerActivities) {
        for (int it = 1; it <= threads; it++) {
            ActivityPoller poller = new ActivityPoller(String.format("activity poller %s", it), SWIFT_DOMAIN, SWIFT_TASK_LIST);
            poller.setSwf(swf);
            poller.addActivities(new StartActivityPollers());
            if (registerActivities && it == 1) {
                poller.registerSwfActivities();
            }
            log.info(String.format("start: %s domain=%s taskList=%s", poller.getId(), SWIFT_DOMAIN, SWIFT_TASK_LIST));
            pool.scheduleWithFixedDelay(poller, 1, 1, TimeUnit.SECONDS);
        }
    }

    @ActivityMethod(name = "Activity Fail Until", version = "1.0",
        description = "input a time in UTC and this activity will fail if called before it"
    )
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
