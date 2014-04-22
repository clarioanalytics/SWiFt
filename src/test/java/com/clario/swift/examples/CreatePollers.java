package com.clario.swift.examples;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.flow.ActivityWorker;
import com.clario.swift.ActivityContext;
import com.clario.swift.ActivityMethod;
import com.clario.swift.ActivityPoller;
import com.clario.swift.DecisionPoller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static java.lang.String.valueOf;
import static java.util.concurrent.TimeUnit.SECONDS;


/**
 * @author George Coller
 */
public class CreatePollers {
    public static final Logger log = LoggerFactory.getLogger(ActivityWorker.class);

    public static void main(String[] args) throws IOException {
        final Config config = Config.getConfig();
        final AmazonSimpleWorkflow swf = config.getAmazonSimpleWorkflow();
        final ExecutorService service = Executors.newFixedThreadPool(config.getPoolSize());
        int threads = config.getPoolSize() / 2;
        startDecisionPoller(service, swf, threads, false);
        startActivityPoller(service, swf, threads, false);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                System.out.println("Shutting Down Simple Workflow Service");
                swf.shutdown();
                System.out.println("Shutting Down Pool");
                service.shutdownNow();
            }
        });
    }

    protected static void startDecisionPoller(ExecutorService pool, AmazonSimpleWorkflow swf, int threads, boolean registerWorkflows) {
        for (int it = 1; it <= threads; it++) {
            String executionContext = System.getProperty("user.name");
            String pollerId = String.format("decision poller %d", it);

            DecisionPoller poller = new DecisionPoller(pollerId, "dev-clario", "default", executionContext);
            poller.setSwf(swf);
            poller.addWorkflows(new DemoWorkflow(), new SimpleWorkflow());
            poller.addWorkflows(new TimerWorkflow(), new WaitForSignalWorkflow());
            poller.addWorkflows(new StartChildWorkflow(), new WaitForSignalWorkflow());
            if (registerWorkflows && it == 1) {
                poller.registerSwfWorkflows();
            }
            pool.submit(poller);
        }
    }

    protected static void startActivityPoller(ExecutorService pool, AmazonSimpleWorkflow swf, int threads, boolean registerActivities) {
        for (int it = 1; it <= threads; it++) {
            ActivityPoller poller = new ActivityPoller(String.format("activity poller %s", it), "dev-clario", "default");
            poller.setSwf(swf);
            poller.addActivities(new CreatePollers());
            if (registerActivities && it == 1) {
                poller.registerSwfActivities();
            }
            log.info("start: " + poller.getId());
            pool.submit(poller);
        }
    }


    @ActivityMethod(name = "Activity X", version = "1.0")
    public void activityX(ActivityContext context) {
        final int i = Integer.parseInt(context.getInput());
        sleep(SECONDS.toMillis(2));
        context.setOutput(valueOf(i + 1));
    }

    @ActivityMethod(name = "Activity Y", version = "1.0")
    public void activityY(ActivityContext context) {
        final int i = Integer.parseInt(context.getInput());
        sleep(SECONDS.toMillis(2));
        context.setOutput(valueOf(i + 100));
    }

    @ActivityMethod(name = "Activity Z", version = "1.0")
    public void activityZ(final ActivityContext context) {
        final int i = Integer.parseInt(context.getInput());
        sleep(SECONDS.toMillis(5));
        log.info("Activity Z " + context.getId() + ": record heartbeat");
        context.recordHeartbeat("some deets: " + valueOf(new Date()));
        sleep(SECONDS.toMillis(10));
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
