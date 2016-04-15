package com.clario.swift.examples;

import com.clario.swift.DecisionPoller;
import com.clario.swift.examples.workflows.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static com.clario.swift.examples.Config.config;


/**
 * Launch a pool of {@link DecisionPoller} and register the example workflows on each poller instance.
 *
 * @author George Coller
 */
public class DecisionPollerPool {
    private static final Logger log = LoggerFactory.getLogger(DecisionPollerPool.class);

    public static void main(String[] args) throws IOException, InterruptedException {
        final ScheduledExecutorService service = Executors.newScheduledThreadPool(config().getDecisionPoolSize());

        for (int it = 1; it <= config().getDecisionPoolSize(); it++) {
            String executionContext = System.getProperty("user.name");
            String pollerId = String.format("decision poller %d", it);

            DecisionPoller poller = new DecisionPoller(pollerId, config().getDomain(), config().getTaskList(), executionContext);
            poller.setSwf(config().getSWF());
            poller.addWorkflows(new CronWorkflow());
            poller.addWorkflows(new PollingCheckpointWorkflow());
            poller.addWorkflows(new RetryActivityWorkflow());
            poller.addWorkflows(new SignalWaitForSignalWorkflow());
            poller.addWorkflows(new SimpleWorkflow());
            poller.addWorkflows(new SimpleWorkflowDecisionBuilder());
            poller.addWorkflows(new SplitJoinWorkflow());
            poller.addWorkflows(new StartChildWorkflow());
            poller.addWorkflows(new TimerWorkflow());
            poller.addWorkflows(new WaitForSignalWorkflow());

            if (config().isRegisterWorkflows() && it == 1) {
                poller.registerSwfWorkflows();
            }
            log.info(String.format("start: %s domain=%s taskList=%s", poller.getId(), config().getDomain(), config().getTaskList()));
            service.scheduleWithFixedDelay(poller, 1, 1, TimeUnit.SECONDS);
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                log.info("Shutting down pool and exiting.");
                try {
                    config().getSWF().shutdown();
                } finally {
                    service.shutdownNow();
                }
            }
        });
        log.info("decision pollers started:");
    }
}
