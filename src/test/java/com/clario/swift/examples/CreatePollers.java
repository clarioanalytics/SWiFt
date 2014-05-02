package com.clario.swift.examples;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.clario.swift.ActivityContext;
import com.clario.swift.ActivityMethod;
import com.clario.swift.ActivityPoller;
import com.clario.swift.DecisionPoller;
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
 * @author George Coller
 */
public class CreatePollers {
    private static final Logger log = LoggerFactory.getLogger(CreatePollers.class);

    public static void main(String[] args) throws IOException, InterruptedException {
        final Config config = getConfig();
        final AmazonSimpleWorkflow swf = config.getAmazonSimpleWorkflow();
        int poolSize = config.getPoolSize() + 1;
        final ScheduledExecutorService service = Executors.newScheduledThreadPool(poolSize);
        startDecisionPoller(service, swf, poolSize / 2, false);
        startActivityPoller(service, swf, poolSize / 2, false);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                System.out.println("Shutting Down Simple Workflow Service");
                swf.shutdown();
                System.out.println("Shutting Down Pool");
                service.shutdownNow();
            }
        });
    }

    protected static void startDecisionPoller(ScheduledExecutorService pool, AmazonSimpleWorkflow swf, int threads, boolean registerWorkflows) {
        for (int it = 1; it <= threads; it++) {
            String executionContext = System.getProperty("user.name");
            String pollerId = String.format("decision poller %d", it);

            DecisionPoller poller = new DecisionPoller(pollerId, SWIFT_DOMAIN, SWIFT_TASK_LIST, executionContext);
            poller.setSwf(swf);
            poller.addWorkflows(new SimpleWorkflow());
            poller.addWorkflows(new TimerWorkflow());
            poller.addWorkflows(new PollingCheckpointWorkflow());
            poller.addWorkflows(new StartChildWorkflow());
            poller.addWorkflows(new WaitForSignalWorkflow());
            poller.addWorkflows(new SignalWaitForSignalWorkflow());
            poller.addWorkflows(new RetryActivityWorkflow());
            if (registerWorkflows && it == 1) {
                poller.registerSwfWorkflows();
            }
            log.info(String.format("start: %s domain=%s taskList=%s", poller.getId(), SWIFT_DOMAIN, SWIFT_TASK_LIST));
            pool.scheduleWithFixedDelay(poller, 1, 1, TimeUnit.SECONDS);
        }
    }

    protected static void startActivityPoller(ScheduledExecutorService pool, AmazonSimpleWorkflow swf, int threads, boolean registerActivities) {
        for (int it = 1; it <= threads; it++) {
            ActivityPoller poller = new ActivityPoller(String.format("activity poller %s", it), SWIFT_DOMAIN, SWIFT_TASK_LIST);
            poller.setSwf(swf);
            poller.addActivities(new CreatePollers());
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
