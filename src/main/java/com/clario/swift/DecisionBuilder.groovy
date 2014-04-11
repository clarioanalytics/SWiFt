package com.clario.swift

import com.amazonaws.services.simpleworkflow.model.Decision
import com.amazonaws.services.simpleworkflow.model.EventType

/**
 * @author George Coller
 */
public class DecisionBuilder {
    private List<DecisionStep> steps = []
    private DecisionStep currentParent
    private DecisionStep lastDecisionGroup
    private int decisionGroupCounter = 0
    private int peerGroupCounter = 0

    List<DecisionStep> getSteps() {
        return steps
    }

    /**
     * Groups a set of decisions as peers so that each added child is added to all decisions in the group.
     * @param peersClosure defines the peers in the group
     * @param childrenOfPeersClosure each decision will be added as a child to all peers
     * @return this instance for chaining
     */
    DecisionBuilder peers(Closure peersClosure, Closure childrenOfPeersClosure = null) {
        def peerStep = new PeerStep("peerGroup${peerGroupCounter++}")
        addStep(peerStep, peersClosure)

        // gather peers and reset peerStep
        def peers = peerStep.children.findAll { peer ->
            peer.parents.remove(peerStep)
        }
        peerStep.children.clear()

        if (childrenOfPeersClosure) {
            callChildren(childrenOfPeersClosure, peerStep)
            // attach children of peers to peer
            peers.each { DecisionStep peer ->
                peerStep.children.each { DecisionStep childOfPeer ->
                    childOfPeer.parents.remove(peerStep)
                    childOfPeer.addParents(peer)
                }
            }
            peerStep.children.clear()
        }
        peerStep.parents.each { DecisionStep parent ->
            parent.children.remove(peerStep)
            peers.each { DecisionStep child ->
                child.addParents(parent)
            }
        }
        peerStep.parents.clear()
        this
    }

    /**
     * Longer workflows should group decisions logically into decision groups, which indicate to the poller
     * that it can stop polling for <code>HistoryEvent</code> records once it hits a group boundary.
     *
     * For example for decision chain <code>A -> (B, C) -> GROUP 1 -> D</code>, once A, B, and C are complete
     * the poller will not gather history prior to GROUP 1.  The outputs of parents B and C will be
     * recorded in GROUP 1 so that child, D, has access to it.
     *
     * @param c optional closure adding more decisions
     * @return this instance for chaining
     */
    DecisionBuilder decisionGroup(Closure c = null) {
        int oldCount = decisionGroupCounter
        decisionGroupCounter++;
        def step = new DecisionGroupDecisionStep(decisionGroupCounter)
        steps.each {
            if (it.decisionGroup == oldCount && it.children.isEmpty()) {
                step.addParents(it)
            }
        }
        steps.add step
        lastDecisionGroup = step
        callChildren(c, step)
        this
    }

    DecisionBuilder retry(int times, long initialWaitInMillis) {
        if (steps) {
            DecisionStep step = steps.last()
            step.addRetry(times, initialWaitInMillis)
        } else {
            throw new IllegalStateException("Must add a step before calling this method")
        }
        this
    }

    /**
     * Manually add an existing step as a parent to this step.
     *
     * Can be used to create decision graphs
     *
     * @param stepId must be an existing step in the workflow
     * @return this instance for chaining
     */
    DecisionBuilder addParent(String stepId) {
        DecisionStep parent = steps.find { it.stepId == stepId }
        if (!parent) {
            throw new IllegalArgumentException("Step '$stepId' does not exist")
        }
        if (steps.isEmpty()) {
            throw new IllegalStateException("Cannot call join($stepId) here")
        }
        steps.last().addParents(parent)
        this
    }

    /**
     * Shorthand method for adding an activity decision.
     * Can be used after an activity has been added to the workflow once with both name and version.
     *
     * @param stepId workflow-unique id for this decision
     * @param name name of the activity to decide
     * @param c optional closure adding child decisions to this step
     * @return this instance for chaining
     */
    DecisionBuilder activity(String stepId, String name, Closure c = null) {
        try {
            def step = steps.findAll {
                it instanceof ActivityDecisionStep && ((ActivityDecisionStep) it).name == name
            }.last() as ActivityDecisionStep
            activity(stepId, name, step.version, c)
        } catch (ignore) {
            throw new IllegalArgumentException("Activity $name version must be supplied at least once before using this method")
        }
        this
    }

    /**
     * Add an activity decision.
     * @param stepId workflow-unique id for this decision
     * @param name name of the activity to decide
     * @param version version of the activity
     * @param c optional closure adding child decisions to this step
     * @return this instance for chaining
     */
    DecisionBuilder activity(String stepId, String name, String version, Closure c = null) {
        if (steps.find { it.stepId == stepId }) {
            throw new IllegalArgumentException("Step '$stepId' already exists in workflow")
        }
        def step = new ActivityDecisionStep(stepId, name, version)
        addStep(step, c)
    }

    /**
     * Add a {@link DecisionStep} to the workflow.
     * @param step the step
     * @param c optional closure adding child decisions to this step
     * @return this instance for chaining
     */
    DecisionBuilder addStep(DecisionStep step, Closure c = null) {
        step.decisionGroup = decisionGroupCounter
        if (currentParent) {
            step.addParents(currentParent)
        } else if (decisionGroupCounter > 0) {
            step.addParents(lastDecisionGroup)
        }
        if (!(step instanceof PeerStep)) {
            steps.add(step);
        }
        callChildren(c, step)
        this
    }

    private void callChildren(Closure c, DecisionStep act) {
        if (c) {
            DecisionStep oldParent = currentParent
            currentParent = act
            c.setDelegate(this)
            c.call()
            currentParent = oldParent
        }
    }

    String toString() {
//        def str = "${DecisionGroupDecisionStep.DECISION_GROUP_PREFIX}0\n"
        steps.collect {
            def str = "$it.stepId"
            if (it instanceof ActivityDecisionStep) {
                ActivityDecisionStep ads = it as ActivityDecisionStep
                str += " '$ads.name'" + ' ' + "'$ads.version'"
            }
            if (it.children) {
                str += " children(${it.children.collect { it.stepId }.join(', ')})"
            }
            if (it.parents) {
                str += " parents(${it.parents.collect { it.stepId }.join(', ')})"
            }
            str
        }.join('\n')
    }

    // Dummy used to handle peer{}
    static class PeerStep extends DecisionStep {
        PeerStep(String stepId) { super(stepId) }

        @Override
        EventType getSuccessEventType() {
            throw new UnsupportedOperationException("Should never be called")
        }

        @Override
        List<EventType> getFailEventTypes() {
            throw new UnsupportedOperationException("Should never be called")
        }

        List<Decision> decide() { throw new UnsupportedOperationException("Should never be called") }
    }
}