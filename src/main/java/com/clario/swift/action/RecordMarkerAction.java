package com.clario.swift.action;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.amazonaws.services.simpleworkflow.model.RecordMarkerDecisionAttributes;
import com.clario.swift.Workflow;

import static com.clario.swift.SwiftUtil.MAX_INPUT_LENGTH;
import static com.clario.swift.SwiftUtil.assertMaxLength;

/**
 * Add a marker to a SWF workflow.
 * <p/>
 * Note: Adding markers do not create additional decision tasks.
 *
 * @author George Coller
 */
public class RecordMarkerAction extends Action<RecordMarkerAction> {

    private String details;

    public RecordMarkerAction(String markerName) {
        super(markerName);
    }

    /**
     * @see RecordMarkerDecisionAttributes#getDetails()
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
