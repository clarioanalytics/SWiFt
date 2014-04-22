package com.clario.swift.action;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.amazonaws.services.simpleworkflow.model.DecisionType;
import com.amazonaws.services.simpleworkflow.model.RecordMarkerDecisionAttributes;

import java.util.List;

/**
 * Record a SWF marker into the history of a workflow run.
 *
 * @author George Coller
 */
public class SwfMarker extends SwfAction {
    private String details;

    public SwfMarker(String id) {
        super(id);
    }

    public SwfMarker withDetails(String details) {
        this.details = details;
        return this;
    }

    @Override
    public boolean decide(List<Decision> decisions) {
        boolean value = super.decide(decisions);
        return ActionState.initial == getState() || value;
    }

    protected Decision createDecision() {
        return new Decision()
            .withDecisionType(DecisionType.RecordMarker)
            .withRecordMarkerDecisionAttributes(new RecordMarkerDecisionAttributes()
                    .withMarkerName(id)
                    .withDetails(details)
            );
    }

    public String getDetails() {
        return details;
    }
}
