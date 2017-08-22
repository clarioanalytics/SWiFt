package com.clario.swift;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.amazonaws.services.simpleworkflow.model.DecisionType;
import com.amazonaws.services.simpleworkflow.model.EventType;
import com.amazonaws.services.simpleworkflow.model.FailWorkflowExecutionDecisionAttributes;
import com.clario.swift.action.Action;
import com.clario.swift.action.ActionSupplier;
import com.fasterxml.jackson.annotation.JsonValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.amazonaws.services.simpleworkflow.model.DecisionType.FailWorkflowExecution;
import static com.clario.swift.DecisionState.*;
import static java.lang.String.format;
import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;

/**
 * Helper class for easily creating complex workflow branching with sequences, split/joins, error handling and a finally block available.
 *
 * @author George Coller
 */
public class DecisionBuilder implements ActionSupplier {

    private static final Logger LOG = LoggerFactory.getLogger(DecisionBuilder.class);
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
    public DecisionState decide() {
        DecisionState result = decideNodes(stack);
        if (result.isFinished() && finallyNode != null) {
            result = decideNodes(singletonList(finallyNode));
        }
        if (result.isFinished() && completeWorkflowExecutionResultSupplier != null) {
            decisions.add(Workflow.createCompleteWorkflowExecutionDecision(completeWorkflowExecutionResultSupplier.get()));
        }
        return result;
    }

    private DecisionState decideNodes(List<Node> nodes) {
        for (Node node : nodes) {
            DecisionState decisionState = node.decideNode();
            if (!decisionState.isSuccess()) {
                return decisionState;
            }
        }
        return DecisionState.success;
    }

    /**
     * Return a json structure representing the stack.
     * Note to get the action ids each Node the ActionSuppliers will be exercised so toString() shouldn't be called
     * if your ActionSupplier happens to be non-idempotent or requires a previous action to be complete.
     */
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

        abstract DecisionState decideNode();

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
    class ActionNode extends Node {
        private final ActionSupplier fn;

        ActionNode(ActionSupplier fn) {
            super(emptyList());
            this.fn = fn;
        }

        @Override public DecisionState decideNode() {
            Action action = fn.get().decide(decisions);
            if (action.isError()) {
                return DecisionState.error;
            } else if (action.isSuccess()) {
                return DecisionState.success;
            } else {
                return notStarted;
            }
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
    class SeqNode extends Node {

        SeqNode(List<Node> nodes) { super(nodes); }

        @Override
        public DecisionState decideNode() {
            for (Node node : nodes) {
                DecisionState decisionState = node.decideNode();
                if (!decisionState.isSuccess()) {
                    return decisionState;
                }
            }
            return DecisionState.success;
        }
    }

    /**
     * Execute child nodes in parallel until all are complete.
     */
    class SplitNode extends Node {

        SplitNode(List<Node> nodes) { super(nodes); }

        @Override
        public DecisionState decideNode() {
            Map<DecisionState, Integer> decisionCountMap = new HashMap<>();
            for (DecisionState state : DecisionState.values()) {
                decisionCountMap.put(state, 0);
            }
            for (Node branch : nodes) {
                decisionCountMap.computeIfPresent(branch.decideNode(), (k, v) -> v + 1);
            }
            if (decisionCountMap.get(notStarted) > 0) {
                return notStarted;
            } else if (decisionCountMap.get(error) > 0) {
                return error;
            } else {
                return success;
            }
        }
    }

    /**
     * Executes a branch of nodes if a given test returns true, otherwise skips.
     */
    class IfThenNode extends Node {
        private final Supplier<Boolean> test;

        IfThenNode(Supplier<Boolean> test, List<Node> nodes) {
            super(nodes);
            this.test = test;
        }

        @Override
        public DecisionState decideNode() {
            DecisionState decisionState = DecisionState.success;
            if (test.get()) {
                decisionState = nodes.get(0).decideNode();
            }
            return decisionState;
        }
    }

    /**
     * Executes a catch block of nodes if one or more nodes in a try block adds a {@link EventType#WorkflowExecutionFailed} decision.
     * The {@link EventType#WorkflowExecutionFailed} decision(s) will be removed.
     */
    class TryCatchNode extends Node {
        TryCatchNode(List<Node> nodes) {
            super(nodes);
            assert nodes.size() == 2 : "assert try catch node has exactly two nodes";
        }

        Node getTryBlock() { return nodes.get(0); }

        Node getCatchBlock() { return nodes.get(1); }

        @Override
        public DecisionState decideNode() {
            DecisionState decisionState = getTryBlock().decideNode();
            if (decisionState.isError()) {
                decisionState = getCatchBlock().decideNode();
                removeFailWorkflowExecutionDecisions();
            }
            return decisionState;
        }
    }

    /**
     * Ensures an execution of a block of nodes regardless if any prior blocks add a {@link EventType#WorkflowExecutionFailed}.
     */
    class AndFinallyNode extends Node {
        AndFinallyNode(List<Node> nodes) {
            super(nodes);
            assert nodes.size() == 1 : "Finally should have exactly one node";
        }

        @Override
        public DecisionState decideNode() {
            DecisionState finallyDecisionState = nodes.get(0).decideNode();
            DecisionState returnState = finallyDecisionState;
            if (finallyDecisionState.isPending()) {
                // remove any fail executors until finally has passed.
                removeFailWorkflowExecutionDecisions();
            } else if (finallyDecisionState.isSuccess()) {
                // if finally success, then return error/success depending on if any prior nodes put in a fail workflow decision.
                // note that prior nodes could be in an error state but have withNoFailWorkflowOnError set.
                returnState = getFailWorkflowDecisionAttributes().isPresent() ? DecisionState.error : DecisionState.success;
            }

            return returnState;
        }
    }

    /**
     * Remove any existing {@link DecisionType#FailWorkflowExecution} decisions.
     */
    public void removeFailWorkflowExecutionDecisions() {
        decisions.removeAll(findDecisions(FailWorkflowExecution));
    }

    /**
     * Return the {@link FailWorkflowExecutionDecisionAttributes} for the first
     * {@link DecisionType#FailWorkflowExecution} decision (if any).
     * <p>
     * Useful in try/catch or finally sections to review the reason/details for a workflow error.
     *
     * @return attributes if they exist
     */
    public Optional<FailWorkflowExecutionDecisionAttributes> getFailWorkflowDecisionAttributes() {
        return findDecisionStream(FailWorkflowExecution).map(Decision::getFailWorkflowExecutionDecisionAttributes).findFirst();
    }

    public List<Decision> findDecisions(DecisionType decisionType) {
        return findDecisionStream(decisionType).collect(toList());
    }

    private Stream<Decision> findDecisionStream(DecisionType decisionType) {
        return decisions.stream()
                   .filter(d -> d.getDecisionType().equals(decisionType.name()));
    }
}
