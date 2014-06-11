package com.clario.swift.examples.workflows;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.clario.swift.Workflow;
import com.clario.swift.action.ActivityAction;
import com.clario.swift.action.RecordMarkerAction;
import com.clario.swift.examples.Config;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.clario.swift.examples.Config.SWIFT_DOMAIN;
import static com.clario.swift.examples.Config.SWIFT_TASK_LIST;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * Example on how you can shortcut loading history events if you have what you need to make the next decision,
 * which will improve performance on workflows that generate thousands of events.
 *
 * @author George Coller
 */
public class PollingCheckpointWorkflow extends Workflow {
    private final Logger log = LoggerFactory.getLogger(PollingCheckpointWorkflow.class);

    public static void main(String[] args) {
        Workflow workflow = new PollingCheckpointWorkflow()
            .withDomain(SWIFT_DOMAIN)
            .withTaskList(SWIFT_TASK_LIST)
            .withExecutionStartToCloseTimeout(MINUTES, 5)
            .withTaskStartToCloseTimeout(MINUTES, 1);
        Config.getConfig().submit(workflow, "100");
    }

    private final RecordMarkerAction doOnceMarkerAction = new RecordMarkerAction("doOnceMarkerAction");

    // Just some no-op activities
    private final ActivityAction step1 = new ActivityAction("step1", "Activity Echo", "1.0").withInput("activity step 1");
    private final ActivityAction step2 = new ActivityAction("step2", "Activity Echo", "1.0").withInput("activity step 2");
    private final ActivityAction step3 = new ActivityAction("step3", "Activity Echo", "1.0").withInput("activity step 3");

    public PollingCheckpointWorkflow() {
        super("Polling Checkpoint Workflow", "1.0");
        addActions(doOnceMarkerAction, step1, step2, step3);
    }

    @Override
    public boolean isContinuePollingForHistoryEvents() {
        // if step2 is completed, we don't need event history for step1 or doOnceMarkerActions, we can go directly to step3
        log.info("number of events: {}", getWorkflowHistory().getActionEvents().size());
        log.info("step2.isSuccess: {}", step2.isSuccess());
        return !step2.isSuccess();
    }

    @Override
    public void decide(List<Decision> decisions) {
        // Sequence of events
        // step 1 -> fill event history -> step 2 -> step 3

        // jump ahead if step 2 is finished
        if (!step2.isSuccess()) {
            if (step1.decide(decisions).isSuccess()) {

                if (!doOnceMarkerAction.isSuccess()) {
                    // Create a ton of markers to fill up the event history
                    doOnceMarkerAction.decide(decisions);
                    for (int i = 0; i < 2000; i++) {
                        decisions.add(doOnceMarkerAction.withDetails(format("marker %d", i)).createInitiateActivityDecision());
                    }
                }
                step2.decide(decisions);
            }
        } else {
            // probably more like 98 or so but certainly not more than a thousand, which proves we've shortcut the history polling.
            assert workflowHistory.getActionEvents().size() < 200;
            step3.withCompleteWorkflowOnSuccess().decide(decisions);
        }
    }
}
