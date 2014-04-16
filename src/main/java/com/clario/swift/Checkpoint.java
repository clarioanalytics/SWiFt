package com.clario.swift;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.amazonaws.services.simpleworkflow.model.EventType;
import com.amazonaws.services.simpleworkflow.model.HistoryEvent;
import com.amazonaws.services.simpleworkflow.model.SignalExternalWorkflowExecutionDecisionAttributes;

import java.util.List;

import static com.amazonaws.services.simpleworkflow.model.DecisionType.SignalExternalWorkflowExecution;
import static com.amazonaws.services.simpleworkflow.model.EventType.WorkflowExecutionSignaled;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

/**
 * Indicates to the poller that {@link HistoryEvent}s for tasks prior to this checkpoint
 * do not need to be read in.
 * <p/>
 * Checkpoints should be used with workflows that generate many {@link HistoryEvent}s since only 1000 events
 * per request are returned by Amazon SWF.
 * <p/>
 * By default this task will use all parent outputs as input.
 *
 * @author George Coller
 */
public class Checkpoint extends Task {
    public static final String CHECKPOINT_PREFIX = "CHECKPOINT:";

    public Checkpoint(int counter) {
        super(CHECKPOINT_PREFIX + counter);
        setCheckpoint(counter);
    }

    @Override
    public EventType getSuccessEventType() {
        return WorkflowExecutionSignaled;
    }

    @Override
    public List<EventType> getFailEventTypes() { return emptyList(); }

    /**
     * Default implementation is to schedule this instances activity with all available inputs
     * serialized using this instance's {@link com.clario.swift.MapSerializer}.
     */
    @Override
    public List<Decision> decide() {
        String output = getIoSerializer().marshal(getInputs());
        return asList(createSignalExternalWorkflowExecution(output, null));
    }

    public static int parseId(String marker) {
        String num = marker.replaceFirst(CHECKPOINT_PREFIX, "");
        return Integer.parseInt(num);
    }

    public Decision createSignalExternalWorkflowExecution(String output, String control) {
        return new Decision()
            .withDecisionType(SignalExternalWorkflowExecution)
            .withSignalExternalWorkflowExecutionDecisionAttributes(
                new SignalExternalWorkflowExecutionDecisionAttributes()
                    .withControl(control)
                    .withSignalName(getId())
                    .withInput(output)
                    .withRunId(getHistoryInspector().getRunId())
                    .withWorkflowId(getHistoryInspector().getWorkflowId())
            );
    }
}
