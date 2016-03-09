package com.clario.swift;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.amazonaws.services.simpleworkflow.model.DecisionType;
import com.clario.swift.action.Action;
import com.clario.swift.action.ActionSupplier;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.amazonaws.services.simpleworkflow.model.DecisionType.FailWorkflowExecution;
import static java.lang.String.format;
import static java.util.Collections.*;

/**
 * Helper class for easily creating complex workflow branching with sequences, split/joins, error handling and a finally block available.
 *
 * @author George Coller
 */
public class DecisionBuilder implements ActionSupplier {
    private final List<Decision> decisions;
    private final Stack<Node> stack = new Stack<>();
    private Node finallyNode;

    /**
     * Instantiate an instance with the list to receive decisions.
     *
     * @param decisions list to receive decisions
     */
    public DecisionBuilder(List<Decision> decisions) {
        this.decisions = decisions;
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
        convertAndPush(CatchNode::new, trySupplier, catchSupplier);
        return this;
    }

    /**
     * Perform a set of actions after all other actions are completed, even if they caused a {@link DecisionType#FailWorkflowExecution} decision.
     * Note that any {@link DecisionType#FailWorkflowExecution} decisions will still be added back after the finally section has successfully completed.
     *
     * @param supplier supplier to perform at the end of a workflow, regardless of any workflow errors.
     */
    public DecisionBuilder doFinally(ActionSupplier supplier) {
        convertAndPush(FinallyNode::new, supplier);
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
                list.add(new ActionFnNode(callback));
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
        boolean result = decideNodes(stack);
        List<Decision> failWorkflowDecisions = findFailWorkflowDecisions(decisions);
        if ((result || !failWorkflowDecisions.isEmpty()) && finallyNode != null) {
            result = decideNodes(singletonList(finallyNode));
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

    /**
     * Print out a string representation of this instance
     *
     * @return this instance
     */
    public DecisionBuilder print() {
        System.out.println(toString());
        return this;
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
        final String type;
        final List<Node> nodes;

        Node(String type, List<Node> nodes) {
            this.type = type;
            this.nodes = nodes;
        }

        abstract boolean decideNode();

        @JsonValue
        Object jsonValue() {
            return singletonMap(type, nodes);
        }

        public String toString() {
            return jsonValue().toString();
        }
    }

    private class ActionFnNode extends Node {
        private final ActionSupplier fn;

        ActionFnNode(ActionSupplier fn) {
            super("fn", emptyList());
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

    private class SeqNode extends Node {

        SeqNode(List<Node> nodes) { super("seq", nodes); }

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

    private class SplitNode extends Node {

        SplitNode(List<Node> nodes) { super("split", nodes); }

        @Override
        public boolean decideNode() {
            boolean result = true;
            for (Node branch : nodes) {
                result &= branch.decideNode();
            }
            return result;
        }
    }

    private class IfThenNode extends Node {

        private final Supplier<Boolean> test;

        IfThenNode(Supplier<Boolean> test, List<Node> nodes) {
            super("ifThen", nodes);
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

    private class CatchNode extends Node {
        CatchNode(List<Node> nodes) { super("tryCatch", nodes); }

        Node getTryBlock() {return nodes.get(0);}

        Node getCatchBlock() {return nodes.get(1);}

        @Override
        public boolean decideNode() {
            boolean result = getTryBlock().decideNode();
            List<Decision> failWorkflowDecisions = findFailWorkflowDecisions(decisions);
            if (!failWorkflowDecisions.isEmpty()) {
                decisions.removeAll(failWorkflowDecisions);
                result = getCatchBlock().decideNode();
            }
            return result;
        }
    }

    private class FinallyNode extends Node {
        FinallyNode(List<Node> nodes) { super("finally", nodes); }

        Node getFinallyNode() {
            assert nodes.size() == 1 : "Finally should be a single node";
            return nodes.get(0);
        }

        @Override
        public boolean decideNode() {
            List<Decision> failWorkflowDecisions = findFailWorkflowDecisions(decisions);
            decisions.removeAll(failWorkflowDecisions);
            boolean result = getFinallyNode().decideNode();
            if (!result) {
                return false;
            } else {
                decisions.addAll(failWorkflowDecisions);
                return true;
            }
        }
    }

    static List<Decision> findFailWorkflowDecisions(List<Decision> decisions) {
        return decisions.stream()
                   .filter(d -> d.getDecisionType().equals(FailWorkflowExecution.name()))
                   .collect(Collectors.toList());
    }

}
