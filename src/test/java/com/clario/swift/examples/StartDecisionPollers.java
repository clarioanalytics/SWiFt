package com.clario.swift.examples;

import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.clario.swift.DecisionPoller;
import com.clario.swift.examples.workflows.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.clario.swift.examples.Config.*;


/**
 * Launch a few decision pollers registered with the example workflows.
 *
 * @author George Coller
 */
public class StartDecisionPollers {
    private static final Logger log = LoggerFactory.getLogger(StartDecisionPollers.class.getSimpleName());

    public static void main(String[] args) throws IOException, InterruptedException {
        final Config config = getConfig();
        final AmazonSimpleWorkflow swf = config.getSWF();
        final ScheduledExecutorService service = Executors.newScheduledThreadPool(config.getPoolSize());
        startDecisionPoller(service, swf, config.getPoolSize(), config.isRegisterWorkflows());

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
            poller.addWorkflows(new ContinuousWorkflow());
            if (registerWorkflows && it == 1) {
                poller.registerSwfWorkflows();
            }
            log.info(String.format("start: %s domain=%s taskList=%s", poller.getId(), SWIFT_DOMAIN, SWIFT_TASK_LIST));
            pool.scheduleWithFixedDelay(poller, 1, 1, TimeUnit.SECONDS);
        }
    }
}
