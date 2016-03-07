package com.clario.swift;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.amazonaws.services.simpleworkflow.model.DecisionType;
import com.clario.swift.action.ActionCallback;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Stack;
import java.util.function.Function;
import java.util.stream.Collectors;

import static com.amazonaws.services.simpleworkflow.model.DecisionType.FailWorkflowExecution;
import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;

/**
 * @author George Coller
 */
public class DecisionBuilder {
    private final List<Decision> decisions;
    private final Stack<Node> nodeStack = new Stack<>();
    private Node finallyNode;

    public DecisionBuilder(List<Decision> decisions) {
        this.decisions = decisions;
    }

    public DecisionBuilder sequence(Object... objects) {
        convertAndPush(nodeStack, objects, SeqNode::new);
        return this;
    }

    public DecisionBuilder split(Object... objects) {
        convertAndPush(nodeStack, objects, SplitNode::new);
        return this;
    }

    /**
     * Perform a set of actions only if one or more {@link DecisionType#FailWorkflowExecution} decisions
     * have been added to the decision list by preceding actions.
     * The {@link DecisionType#FailWorkflowExecution} decisions are removed and replaced with decisions
     * made from the given actions.
     *
     * @param tryBlock try block of actions
     * @param catchBlock catch block of actions
     */
    public DecisionBuilder tryCatch(Object tryBlock, Object catchBlock) {
        Object[] objects = Arrays.asList(tryBlock, catchBlock).toArray();
        convertAndPush(nodeStack, objects, CatchNode::new);
        return this;
    }

    /**
     * Perform a set of actions after all other actions are completed, even if they caused a {@link DecisionType#FailWorkflowExecution} decision.
     * Note that any {@link DecisionType#FailWorkflowExecution} decisions will still be added back after the finally section has successfully completed.
     */
    public DecisionBuilder doFinally(Object finallyBlock) {
        convertAndPush(nodeStack, singletonList(finallyBlock).toArray(), FinallyNode::new);
        finallyNode = nodeStack.pop();
        return this;
    }

    private Node convertAndPush(Stack<Node> stack, Object[] objects, Function<List<Node>, Node> fn) {
        if (finallyNode != null) {
            throw new IllegalStateException("There can only be exactly one finally block allowed and it must be the last block added");
        }
        // pre-pop arguments to preserve argument ordering.
        Stack<Node> popList = new Stack<>();
        for (Object o : objects) {
            if (o == this) {
                popList.push(stack.pop());
            }
        }

        // create full arg list 
        List<Node> list = new ArrayList<>();
        for (Object o : objects) {
            if (o instanceof ActionCallback) {
                list.add(new ActionFnNode((ActionCallback) o));
            } else if (o == this) {
                list.add(popList.pop());
            } else {
                throw new IllegalArgumentException(format("Unexpected object on stack: %s", o));
            }
        }
        Node node = fn.apply(list);
        stack.push(node);
        return node;
    }

    /**
     * @return true if isSuccess(), otherwise false.
     */
    public boolean decide() {
        boolean result = decideNodes(nodeStack);
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

    public DecisionBuilder print() {
        System.out.println(SwiftUtil.toJson(nodeStack, true));
        return this;
    }

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
            return String.format("%s(%s)", type, join(",", nodes.stream().map(Object::toString).collect(toList())));
        }
    }

    private class ActionFnNode extends Node {
        private final ActionCallback fn;

        ActionFnNode(ActionCallback fn) {
            super("fn", emptyList());
            this.fn = fn;
        }

        @Override public boolean decideNode() {
            return fn.apply().decide(decisions).isSuccess();
        }

        @Override public String toString() {
            return format("'%s'", fn.apply().getActionId());
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
            boolean finishedFlag = true;
            for (Node branch : nodes) {
                finishedFlag &= branch.decideNode();
            }
            return finishedFlag;
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
        return findDecisions(decisions, FailWorkflowExecution);
    }

    static List<Decision> findDecisions(List<Decision> decisions, DecisionType decisionType) {
        return decisions.stream().filter(d -> d.getDecisionType().equals(decisionType.name())).collect(Collectors.toList());
    }
}
