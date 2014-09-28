package com.clario.swift.examples;

import com.clario.swift.ActivityContext;
import com.clario.swift.ActivityMethod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;

import static java.lang.Integer.parseInt;
import static java.lang.String.valueOf;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Activity methods used by the example workflows.
 * <p/>
 * Demonstrates that a single class may define more than one activity.
 * In an actual project activity classes will probably have dependencies on external services, etc.
 *
 * @author George Coller
 */
public class ActivityMethods {
    private static final Logger log = LoggerFactory.getLogger(DecisionPollerPool.class);
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

    @ActivityMethod(name = "Activity Echo With Pause", version = "1.0", description = "Pause and log input seconds and return input",
        scheduleToCloseTimeout = "NONE", scheduleToStartTimeout = "NONE", startToCloseTimeout = "60")
    public String activityPause(ActivityContext context) {
        String input = context.getInput();
        sleep(SECONDS.toMillis(parseInt(input)));
        log.info(input);
        return input;
    }

    @ActivityMethod(name = "Activity X", version = "1.0")
    public Integer activityX(ActivityContext context) {
        final int i = parseInt(context.getInput());
        sleep(SECONDS.toMillis(2));
        return i + 1;
    }

    @ActivityMethod(name = "Activity Y", version = "1.0")
    public Integer activityY(ActivityContext context) {
        final int i = parseInt(context.getInput());
        sleep(SECONDS.toMillis(2));
        return i + 100;
    }

    @ActivityMethod(name = "Activity Z", version = "1.0")
    public Integer activityZ(final ActivityContext context) {
        final int i = parseInt(context.getInput());
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
