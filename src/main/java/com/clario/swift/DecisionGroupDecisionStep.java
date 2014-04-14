package com.clario.swift;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.amazonaws.services.simpleworkflow.model.DecisionType;
import com.amazonaws.services.simpleworkflow.model.EventType;
import com.amazonaws.services.simpleworkflow.model.SignalExternalWorkflowExecutionDecisionAttributes;

import java.util.Collections;
import java.util.List;

import static com.amazonaws.services.simpleworkflow.model.EventType.WorkflowExecutionSignaled;
import static java.util.Arrays.asList;

/**
 * Record a signal indicating that all {@link DecisionStep} before this one are complete and their history events do
 * not need to be read in by the poller.
 * <p/>
 * By default this step will use all parent outputs as input.
 *
 * @author George Coller
 */
public class DecisionGroupDecisionStep extends DecisionStep {
    public DecisionGroupDecisionStep(int counter) {
        super(DECISION_GROUP_PREFIX + counter);
        setDecisionGroup(counter);
    }

    @Override
    public EventType getSuccessEventType() {
        return WorkflowExecutionSignaled;
    }

    @Override
    public List<EventType> getFailEventTypes() {
        return Collections.emptyList();
    }

    @Override
    public List<StepEvent> stepEvents() {
        return super.stepEvents();
    }

    /**
     * Default implementation is to schedule this instances activity with all available inputs
     * serialized using this instance's {@link com.clario.swift.MapSerializer}.
     */
    @Override
    public List<Decision> decide() {
        String output = getIoSerializer().marshal(getInputs());
        return asList(createSignalExternalWorkflowExecution(output));
    }

    public static int parseStepId(String marker) {
        String num = marker.replaceFirst(DECISION_GROUP_PREFIX, "");
        return Integer.parseInt(num);
    }

    public Decision createSignalExternalWorkflowExecution(String output, String control) {
        return new Decision().withDecisionType(DecisionType.SignalExternalWorkflowExecution).withSignalExternalWorkflowExecutionDecisionAttributes(new SignalExternalWorkflowExecutionDecisionAttributes().withControl(control).withSignalName(getStepId()).withInput(output).withRunId(getHistoryInspector().getRunId()).withWorkflowId(getHistoryInspector().getWorkflowId()));
    }

    public Decision createSignalExternalWorkflowExecution(String output) {
        return createSignalExternalWorkflowExecution(output, null);
    }

    public static final String DECISION_GROUP_PREFIX = "DecisionGroup:";
}
