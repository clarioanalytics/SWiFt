package com.clario.swift;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.amazonaws.services.simpleworkflow.model.DecisionType;
import com.amazonaws.services.simpleworkflow.model.EventType;
import com.clario.swift.action.Action;
import com.clario.swift.action.ActionSupplier;
import com.fasterxml.jackson.annotation.JsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Stack;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.amazonaws.services.simpleworkflow.model.DecisionType.FailWorkflowExecution;
import static java.lang.String.format;
import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;

/**
 * Helper class for easily creating complex workflow branching with sequences, split/joins, error handling and a finally block available.
 *
 * @author George Coller
 */
public class DecisionBuilder implements ActionSupplier {
    private static final Logger log = LoggerFactory.getLogger(DecisionBuilder.class);
    private final List<Decision> decisions;
    private final Stack<Node> stack = new Stack<>();
    private Node finallyNode;
    private Supplier<String> completeWorkflowExecutionResultSupplier;

    /**
     * Instantiate an instance with the list to receive decisions.
     *
     * @param decisions list to receive decisions
     */
    public DecisionBuilder(List<Decision> decisions) {
        this.decisions = decisions;
    }

    /**
     * Make a {@link DecisionType#CompleteWorkflowExecution} decision using the given supplier
     * after the workflow decision tree has been fully exercised.
     *
     * @param result use this supplier to create a final workflow execution decision
     *
     * @return this instance
     */
    public DecisionBuilder withCompleteWorkflowExecution(Supplier<String> result) {
        this.completeWorkflowExecutionResultSupplier = result;
        return this;
    }

    /**
     * Perform a set of actions in sequence (one after another).
     *
     * @param suppliers list of {@link ActionSupplier} to sequence
     *
     * @return this instance
     */
    public DecisionBuilder sequence(ActionSupplier... suppliers) {
        convertAndPush(SeqNode::new, suppliers);
        return this;
    }

    /**
     * Perform a set of actions in parallel, joining on the next builder action after the split.
     *
     * @param suppliers list of {@link ActionSupplier} to sequence
     *
     * @return this instance
     */
    public DecisionBuilder split(ActionSupplier... suppliers) {
        convertAndPush(SplitNode::new, suppliers);
        return this;
    }

    /**
     * Perform a set of actions if a test returns true.
     *
     * @param test supplier callback that returns a boolean
     * @param supplier block of actions to be performed if test returns true.
     *
     * @return this instance
     */
    public DecisionBuilder ifThen(Supplier<Boolean> test, ActionSupplier supplier) {
        convertAndPush(nodes -> new IfThenNode(test, nodes), supplier);
        return this;
    }

    /**
     * Perform a set of actions only if one or more {@link DecisionType#FailWorkflowExecution} decisions
     * have been added to the decision list by preceding actions.
     * The {@link DecisionType#FailWorkflowExecution} decisions are removed and replaced with decisions
     * made from the given actions.
     *
     * @param trySupplier try block of actions
     * @param catchSupplier catch block of actions
     */
    public DecisionBuilder tryCatch(ActionSupplier trySupplier, ActionSupplier catchSupplier) {
        convertAndPush(TryCatchNode::new, trySupplier, catchSupplier);
        return this;
    }

    /**
     * Perform a set of actions after all other actions are completed, even if they caused a {@link DecisionType#FailWorkflowExecution} decision.
     *
     * @param supplier supplier to perform at the end of a workflow, regardless of any workflow errors.
     */
    public DecisionBuilder andFinally(ActionSupplier supplier) {
        convertAndPush(AndFinallyNode::new, supplier);
        finallyNode = stack.pop();
        return this;
    }

    // common method to pop all arguments to fn off stack and push fn(args) back on stack
    private Node convertAndPush(Function<List<Node>, Node> fn, ActionSupplier... callbacks) {
        if (finallyNode != null) {
            throw new IllegalStateException("There can only be exactly one finally block allowed and it must be the last block added");
        }
        // pre-pop arguments to preserve original argument ordering.
        Stack<Node> popList = new Stack<>();
        for (Object o : callbacks) {
            if (o == this) {
                popList.push(stack.pop());
            }
        }

        // create full arg list 
        List<Node> list = new ArrayList<>();
        for (ActionSupplier callback : callbacks) {
            if (callback == this) {
                list.add(popList.pop());
            } else {
                list.add(new ActionNode(callback));
            }
        }
        Node node = fn.apply(list);
        stack.push(node);
        return node;
    }

    /**
     * Make the next set of decisions given the current workflow state.
     *
     * @return true if the decision tree is finished, otherwise false.
     */
    public boolean decide() {
        log.debug(toString());
        boolean result = decideNodes(stack);
        List<Decision> failWorkflowDecisions = findFailWorkflowDecisions(decisions);
        if ((result || !failWorkflowDecisions.isEmpty()) && finallyNode != null) {
            result = decideNodes(singletonList(finallyNode));
        }
        if (result && completeWorkflowExecutionResultSupplier != null) {
            decisions.add(Workflow.createCompleteWorkflowExecutionDecision(completeWorkflowExecutionResultSupplier.get()));
        }
        return result;
    }

    private boolean decideNodes(List<Node> nodes) {
        for (Node node : nodes) {
            if (!node.decideNode()) {
                return false;
            }
        }
        return true;
    }

    public String toString() {
        return SwiftUtil.toJson(stack, false);
    }

    @Override
    public Action get() {
        // ActionSupplier is implemented so the DecisionBuilder instance can be a builder parameter
        // This is a bit jank but it gets the job done without forcing clients to have to cast their lambdas.
        throw new UnsupportedOperationException("method not available for " + getClass().getSimpleName());
    }


    //---------------------------------------------------------------------------------------------------- 
    // Workflow decisions are a tree of Nodes
    //---------------------------------------------------------------------------------------------------- 
    abstract class Node {
        final List<Node> nodes;

        Node(List<Node> nodes) {
            this.nodes = nodes;
        }

        abstract boolean decideNode();

        @JsonValue
        Object jsonValue() {
            return singletonMap(getClass().getSimpleName().replace("Node", ""), nodes);
        }

        public String toString() {
            return jsonValue().toString();
        }
    }


    /**
     * Leaf node that calls decide on an {@link Action}.
     */
    private class ActionNode extends Node {
        private final ActionSupplier fn;

        ActionNode(ActionSupplier fn) {
            super(emptyList());
            this.fn = fn;
        }

        @Override public boolean decideNode() {
            return fn.get().decide(decisions).isSuccess();
        }

        @Override public String toString() {
            return format("'%s'", fn.get().getActionId());
        }

        @JsonValue
        Object jsonValue() {
            return toString();
        }
    }

    /**
     * Execute child nodes sequentially.
     */
    private class SeqNode extends Node {

        SeqNode(List<Node> nodes) { super(nodes); }

        @Override
        public boolean decideNode() {
            for (Node node : nodes) {
                if (!node.decideNode()) {
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * Execute child nodes in parallel until all are complete.
     */
    private class SplitNode extends Node {

        SplitNode(List<Node> nodes) { super(nodes); }

        @Override
        public boolean decideNode() {
            boolean result = true;
            for (Node branch : nodes) {
                result &= branch.decideNode();
            }
            return result;
        }
    }

    /**
     * Executes a branch of nodes if a given test returns true, otherwise skips.
     */
    private class IfThenNode extends Node {
        private final Supplier<Boolean> test;

        IfThenNode(Supplier<Boolean> test, List<Node> nodes) {
            super(nodes);
            this.test = test;
        }

        @Override
        public boolean decideNode() {
            boolean result = true;
            if (test.get()) {
                result = nodes.get(0).decideNode();
            }
            return result;
        }
    }

    /**
     * Executes a catch block of nodes if one or more nodes in a try block adds a {@link EventType#WorkflowExecutionFailed} decision.
     * The {@link EventType#WorkflowExecutionFailed} decision(s) will be removed.
     */
    private class TryCatchNode extends Node {
        TryCatchNode(List<Node> nodes) {
            super(nodes);
            assert nodes.size() == 2 : "assert try catch node has exactly two nodes";
        }

        Node getTryBlock() { return nodes.get(0); }

        Node getCatchBlock() { return nodes.get(1); }

        @Override
        public boolean decideNode() {
            boolean result = getTryBlock().decideNode();
            List<Decision> failWorkflowDecisions = findFailWorkflowDecisions(decisions);
            if (!failWorkflowDecisions.isEmpty()) {
                result = getCatchBlock().decideNode();
                decisions.removeAll(failWorkflowDecisions);
            }
            return result;
        }
    }

    /**
     * Ensures an execution of a block of nodes regardless if any prior blocks add a {@link EventType#WorkflowExecutionFailed}.
     */
    private class AndFinallyNode extends Node {
        AndFinallyNode(List<Node> nodes) {
            super(nodes);
            assert nodes.size() == 1 : "Finally should have exactly one node";
        }

        Node getFinallyNode() { return nodes.get(0); }

        // TODO: finally block (and catch block) need access to failWorkflowDecision errors
        // so they can do something with them.
        // TODO: consider removing "keepFailDecisions" parameter and let finally block handle it 
        // explicitly since any instance of FailWorkflowExecution will stop the workflow so no other
        // decisions can be handled

        @Override
        public boolean decideNode() {
            boolean result = getFinallyNode().decideNode();
            List<Decision> failWorkflowDecisions = findFailWorkflowDecisions(decisions);
            if (!result) {
                decisions.removeAll(failWorkflowDecisions);
            }
            if (!failWorkflowDecisions.isEmpty()) {
                result = false;
            }
            return result;
        }
    }

    public Optional<Decision> findFailWorkflowDecision() {
        return findDecisions(decisions, FailWorkflowExecution).stream().findAny();
    }

    static List<Decision> findFailWorkflowDecisions(List<Decision> decisions) {
        return findDecisions(decisions, FailWorkflowExecution);
    }

    static List<Decision> findDecisions(List<Decision> decisions, DecisionType decisionType) {
        return decisions.stream()
                   .filter(d -> d.getDecisionType().equals(decisionType.name()))
                   .collect(toList());
    }
}
