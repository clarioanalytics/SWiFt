package com.clario.swift.action;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.amazonaws.services.simpleworkflow.model.RecordMarkerDecisionAttributes;
import com.clario.swift.Workflow;

import static com.clario.swift.SwiftUtil.MAX_INPUT_LENGTH;
import static com.clario.swift.SwiftUtil.assertMaxLength;

/**
 * Add a marker to a SWF workflow.
 * <p/>
 * <b>WARNING</b>: Marker actions do not cause an additional decision event to be issued by SWF.
 * <p/>
 * What this means is that workflows that add {@link RecordMarkerAction} decisions must also
 * add an additional decision (another Action or {@link Workflow#createCompleteWorkflowExecutionDecision(String)}
 * or the workflow will get stuck.
 * <p/>
 * Example usage within a {@link Workflow} subclass:
 * <pre><code>
 private final RecordMarkerAction doOnceMarker = new RecordMarkerAction("doOnceMarker");

 ...

 public StartChildWorkflow() {
     ....

     addActions(doOnceMarker);
 }

 public void decide(List<Decision> decisions) {
     if (doOnceMarker.isInitial()) {
         String markerInput = ... // result of some run-once code

         doOnceMarker
              .withDetails(markerInput)
              .decide(decisions)
     }
 }
 * </code></pre>
 *
 *
 * @author George Coller
 * @see com.clario.swift.examples.workflows Example workflows for usage ideas.
 */
public class RecordMarkerAction extends Action<RecordMarkerAction> {

    private String details;

    public RecordMarkerAction(String markerName) {
        super(markerName);
    }

    /**
     * @see RecordMarkerDecisionAttributes#getDetails
     */
    public RecordMarkerAction withDetails(String input) {
        this.details = assertMaxLength(input, MAX_INPUT_LENGTH);
        return this;
    }

    @Override
    public String getOutput() {
        if (isSuccess()) {
            return getCurrentEvent().getData1();
        } else {
            return details;
        }
    }

    @Override
    protected RecordMarkerAction thisObject() {
        return this;
    }

    @Override
    public Decision createInitiateActivityDecision() {
        return Workflow.createRecordMarkerDecision(getActionId(), details);
    }
}
