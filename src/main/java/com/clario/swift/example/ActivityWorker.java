package com.clario.swift.example;

import com.clario.swift.ActivityContext;
import com.clario.swift.ActivityMethod;
import com.clario.swift.ActivityPoller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.TimeUnit;

import static java.lang.String.valueOf;

/**
 * @author George Coller
 */
public class ActivityWorker {
    public static final Logger log = LoggerFactory.getLogger(ActivityWorker.class);

    public static void main(String[] args) throws IOException {
        Config config = Config.getConfig();
        int threads = config.getPoolSize() / 2;
        for (int it = 1; it <= threads; it++) {
            ActivityPoller poller = new ActivityPoller(String.format("activity poller %s", it), "dev-clario", "default");
            poller.setSwf(config.getAmazonSimpleWorkflow());
            poller.addActivities(new ActivityWorker());
            if (it == 1) {
                poller.registerSimpleWorkflowActivities();
            }
            log.info("start: " + poller.getId());
            config.getService().submit(poller);
        }
    }

    @ActivityMethod(name = "Activity X", version = "1.0")
    public void activityX(ActivityContext context) {
        final int i = Integer.parseInt(context.getInput());
        sleep(TimeUnit.SECONDS.toMillis(2));
        context.setOutput(valueOf(i + 1));
    }

    @ActivityMethod(name = "Activity Y", version = "1.0")
    public void activityY(ActivityContext context) {
        final int i = Integer.parseInt(context.getInput());
        sleep(TimeUnit.SECONDS.toMillis(2));
        context.setOutput(valueOf(i + 100));
    }

    @ActivityMethod(name = "Activity Z", version = "1.0")
    public void activityZ(final ActivityContext context) {
        final int i = Integer.parseInt(context.getInput());
        sleep(TimeUnit.SECONDS.toMillis(5));
        log.info("Activity Z " + context.getId() + ": record heartbeat");
        context.recordHeartbeat("some deets: " + valueOf(new Date()));
        sleep(TimeUnit.SECONDS.toMillis(10));
        context.setOutput(valueOf(i + 1000));
    }

    private static void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            throw new IllegalStateException("Interrupted", e);
        }
    }
}
