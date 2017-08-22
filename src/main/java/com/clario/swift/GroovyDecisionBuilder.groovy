package com.clario.swift

import com.amazonaws.services.simpleworkflow.model.Decision
import com.amazonaws.services.simpleworkflow.model.DecisionType
import com.amazonaws.services.simpleworkflow.model.FailWorkflowExecutionDecisionAttributes
import com.clario.swift.action.Action
import com.clario.swift.action.ActionSupplier

import java.util.function.Supplier

/**
 * @author George Coller
 */
class GroovyDecisionBuilder {
    private final DecisionBuilder builder

    GroovyDecisionBuilder(List<Decision> decisions) {
        builder = new DecisionBuilder(decisions)
    }

    GroovyDecisionBuilder withCompleteWorkflowExecution(Closure<String> result) {
        builder.withCompleteWorkflowExecution(result as Supplier<String>)
        this
    }

    private ActionSupplier convert(Object o) {
        if (o == this) {
            return builder
        } else if (o instanceof ActionSupplier || o instanceof Closure<Action>) {
            return o as ActionSupplier
        } else {
            throw new IllegalArgumentException("Object not allowed ${o.class} $o")
        }
    }

    GroovyDecisionBuilder sequence(Object... suppliers) {
        builder.sequence(*suppliers.collect { convert(it) })
        this
    }

    GroovyDecisionBuilder split(Object... suppliers) {
        builder.split(*suppliers.collect { convert(it) })
        this
    }

    GroovyDecisionBuilder ifThen(Closure<Boolean> test, Object supplier) {
        builder.ifThen(test as Supplier<Boolean>, convert(supplier))
        this
    }

    GroovyDecisionBuilder tryCatch(Object trySupplier, Object catchSupplier) {
        builder.tryCatch(convert(trySupplier), convert(catchSupplier))
        this
    }

    GroovyDecisionBuilder andFinally(Object supplier) {
        builder.andFinally(convert(supplier))
        this
    }

    DecisionState decide() {
        builder.decide()
    }

    void removeFailWorkflowExecutionDecisions() {
        builder.removeFailWorkflowExecutionDecisions()
    }

    Optional<FailWorkflowExecutionDecisionAttributes> getFailWorkflowDecisionAttributes() {
        return builder.getFailWorkflowDecisionAttributes()
    }

    List<Decision> findDecisions(DecisionType decisionType) { builder.findDecisions(decisionType) }

    @Override
    String toString() {
        builder.toString()
    }

    Action get() {
        builder.get()
    }
}
