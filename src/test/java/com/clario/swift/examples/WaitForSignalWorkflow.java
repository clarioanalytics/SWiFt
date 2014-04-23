package com.clario.swift.examples;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.clario.swift.SwiftUtil;
import com.clario.swift.Workflow;
import com.clario.swift.action.SwfAction;
import com.clario.swift.action.SwfActivity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static java.util.concurrent.TimeUnit.MINUTES;

/**
 * @author George Coller
 */
public class WaitForSignalWorkflow extends Workflow {
    public static final Logger log = LoggerFactory.getLogger(WaitForSignalWorkflow.class);

    public static void main(String[] args) {
        Workflow workflow = new WaitForSignalWorkflow()
            .withDomain("dev-clario")
            .withTaskList("default")
            .withExecutionStartToCloseTimeout(TimeUnit.MINUTES, 30)
            .withTaskStartToCloseTimeout(TimeUnit.MINUTES, 30);
//        Config.register(workflow);
        Config.submit(workflow, "100");
    }

    private final SwfActivity step1 = new SwfActivity("childStep1", "Activity X", "1.0").withStartToCloseTimeout(MINUTES, 2);

    public WaitForSignalWorkflow() {
        super("Wait For Signal Workflow", "1.0");
        withExecutionStartToCloseTimeout(MINUTES, 30);
    }

    @Override
    public List<SwfAction> getActions() {
        return Arrays.asList((SwfAction) step1);
    }

    @Override
    public void decide(List<Decision> decisions) {
        // Wait until a signal is received, then do a activity
        Map<String, String> signals = getSwfHistory().getSignals();
        if (signals.isEmpty()) {
            log.info("No signal received yet");
        } else {
            String signalValue = new ArrayList<>(signals.values()).get(0);
            if (step1.withInput(signalValue).decide(decisions)) {
                log.info("Signal received and step1 finished");
                decisions.add(SwiftUtil.createCompleteWorkflowExecutionDecision(step1.getOutput()));
            }
        }
    }
}
