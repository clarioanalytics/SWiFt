package com.clario.swift;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.clario.swift.action.ActionCallback;
import com.fasterxml.jackson.annotation.JsonValue;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;
import java.util.function.Function;

import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;

/**
 * @author George Coller
 */
public class DecisionBuilder {
    private final List<Decision> decisions;
    private final Stack<Node> stack = new Stack<>();
    private final List<Node> temp = new ArrayList<>();

    public DecisionBuilder(List<Decision> decisions) {
        this.decisions = decisions;
    }

    public DecisionBuilder sequence(Object... objects) {
        return convertAndPush(objects, SeqNode::new);
    }

    public DecisionBuilder split(Object... objects) {
        return convertAndPush(objects, SplitNode::new);
    }

    private DecisionBuilder convertAndPush(Object[] objects, Function<List<Node>, Node> fn) {
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
        if (list.size() == 1) {
            stack.push(list.get(0));
        } else {
            stack.push(fn.apply(list));
        }
        return this;
    }

    /**
     * @return true if isSuccess(), otherwise false.
     */
    public boolean decide() {
        return decide(stack);
    }

    public DecisionBuilder print() {
        System.out.println(SwiftUtil.toJson(stack, true));
        return this;
    }

    private boolean decide(List<Node> nodes) {
        for (Node node : nodes) {
            if (!node.decide()) {
                return false;
            }
        }
        return true;
    }

    abstract class Node {
        final String type;
        final List<Node> nodes;

        Node(String type, List<Node> nodes) {
            this.type = type;
            this.nodes = nodes;
        }

        abstract boolean decide();

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

        @Override public boolean decide() {
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
        public boolean decide() {
            for (Node node : nodes) {
                if (!node.decide()) {
                    return false;
                }
            }
            return true;
        }
    }

    private class SplitNode extends Node {

        SplitNode(List<Node> nodes) { super("split", nodes); }

        @Override
        public boolean decide() {
            boolean finishedFlag = true;
            for (Node branch : nodes) {
                finishedFlag &= branch.decide();
            }
            return finishedFlag;
        }
    }
}
