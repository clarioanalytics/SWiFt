package com.clario.swift.examples.workflows;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.amazonaws.services.simpleworkflow.model.DecisionType;
import com.clario.swift.DecisionBuilder;
import com.clario.swift.Workflow;
import com.clario.swift.action.ActionFn;
import com.clario.swift.action.ActivityAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

import static com.amazonaws.services.simpleworkflow.model.ChildPolicy.TERMINATE;
import static com.clario.swift.examples.Config.config;
import static java.lang.String.format;
import static java.util.concurrent.TimeUnit.MINUTES;
import static java.util.concurrent.TimeUnit.SECONDS;

/**
 * Example demonstrating how to split a workflow into parallel branches and then join.
 * <p/>
 * <pre><code>
 *              step2 -> step4
 *     step1 ->                -> step6
 *              step3 -> step5
 * </code></pre>
 *
 * @author George Coller
 */
public class SplitJoinWorkflowSequenced extends Workflow {
    private final Logger log = LoggerFactory.getLogger(SplitJoinWorkflowSequenced.class);

    public static void main(String[] args) {
        Workflow workflow = new SplitJoinWorkflowSequenced()
                                .withDomain(config().getDomain())
                                .withTaskList(config().getTaskList())
                                .withExecutionStartToCloseTimeout(MINUTES, 5)
                                .withTaskStartToCloseTimeout(SECONDS, 30)
                                .withChildPolicy(TERMINATE)
                                .withDescription("Example of split/join");
        config().submit(workflow, "");
    }

    final ActivityAction step1 = new ActivityAction("step1", "Activity Echo With Pause", "1.0");
    final ActivityAction step2 = new ActivityAction("step2", "Activity Echo With Pause", "1.0");
    final ActivityAction step3 = new ActivityAction("step3", "Activity Echo With Pause", "1.0");
    final ActivityAction step4 = new ActivityAction("step4", "Activity Echo With Pause", "1.0");
    final ActivityAction step5 = new ActivityAction("step5", "Activity Echo With Pause", "1.0");
    final ActivityAction step6 = new ActivityAction("step6", "Activity Echo", "1.0")
                                     .withCompleteWorkflowOnSuccess();

    public SplitJoinWorkflowSequenced() {
        super("Split Join Workflow", "1.0");
        addActions(step1, step2, step3, step4, step5, step6);
    }


    @Override
    public void decide(List<Decision> decisions) {
        ActionFn f1 = () -> step1.withInput("1");
        ActionFn f2 = () -> step2.withInput("2");
        ActionFn f3 = () -> step3.withInput("3");
        ActionFn f4 = () -> step4.withInput("4");
        ActionFn f5 = () -> step5.withInput("5");
        ActionFn f6 = () -> step6.withInput("6");

        DecisionBuilder d = new DecisionBuilder(decisions);
        d.sequence(f1)
            .split(
                d.sequence(f2, f4),
                d.sequence(f3, f5)
            )
            .sequence(f6)
            .decide();


        if (sequence(decisions, f1)) {
            if (sequence(decisions, f2, f4) &&
                    sequence(decisions, f3, f5)
                ) {
                sequence(decisions, f6);
            }
        }

        log.info("Decisions: ");
        for (Decision decision : decisions) {
            if (DecisionType.ScheduleActivityTask.toString().equals(decision.getDecisionType())) {
                log.info(format("Start activity step%s", decision.getScheduleActivityTaskDecisionAttributes().getInput()));
            } else {
                log.info("Complete Workflow");
            }
        }
    }
}