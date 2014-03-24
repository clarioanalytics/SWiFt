package com.clario.swift

import com.amazonaws.services.simpleworkflow.model.Decision
import com.amazonaws.services.simpleworkflow.model.DecisionType
import com.amazonaws.services.simpleworkflow.model.EventType
import com.amazonaws.services.simpleworkflow.model.SignalExternalWorkflowExecutionDecisionAttributes

import static com.amazonaws.services.simpleworkflow.model.EventType.WorkflowExecutionSignaled

/**
 * Record a signal indicating that all {@link DecisionStep} before this one are complete and their history events do
 * not need to be read in by the poller.
 *
 * By default this step will use all parent outputs as input.
 * @author George Coller
 */
public class DecisionGroupDecisionStep extends DecisionStep {
    public static final String DECISION_GROUP_PREFIX = "DecisionGroup:"

    DecisionGroupDecisionStep(int counter) {
        super(DECISION_GROUP_PREFIX + counter)
        decisionGroup = counter
    }

    @Override
    List<EventType> getFinalEventTypes() {
        return [WorkflowExecutionSignaled]
    }

    @Override
    List<StepEvent> stepEvents() {
        return super.stepEvents()
    }

    /**
     * Default implementation is to schedule this instances activity with all available inputs
     * serialized using this instance's {@link MapSerializer}.
     */
    @Override
    List<Decision> decide() {
        def output = ioSerializer.marshal(inputs)
        [createSignalExternalWorkflowExecution(output)]
    }

    Map<String, String> getOutput() {
        StepEvent stepEvent = stepEvents()?.first()
        if (!(stepEvent && stepEvent.type == WorkflowExecutionSignaled)) {
            throw new IllegalStateException("No result available")
        }
        return ioSerializer.unmarshal(stepEvent.historyEvent.workflowExecutionSignaledEventAttributes.input)
    }

    static int parseStepId(String marker) {
        marker.minus(DECISION_GROUP_PREFIX).toInteger()
    }

    Decision createSignalExternalWorkflowExecution(String output, String control = null) {
        new Decision()
                .withDecisionType(DecisionType.SignalExternalWorkflowExecution)
                .withSignalExternalWorkflowExecutionDecisionAttributes(
                new SignalExternalWorkflowExecutionDecisionAttributes()
                        .withControl(control)
                        .withSignalName(stepId)
                        .withInput(output)
                        .withRunId(historyInspector.runId)
                        .withWorkflowId(historyInspector.workflowId)
        )
    }
}