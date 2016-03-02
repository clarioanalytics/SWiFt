package com.clario.swift;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.clario.swift.action.ActionFn;
import com.clario.swift.action.MockAction;
import com.clario.swift.event.EventState;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.clario.swift.event.EventState.ACTIVE;
import static com.clario.swift.event.EventState.SUCCESS;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.Assert.assertEquals;


/**
 * @author George Coller
 */
public class DeciderTest {

    MockAction s1, s2, s3, s4, s5, s6;
    ActionFn f1, f2, f3, f4, f5, f6;
    List<Decision> decisions;
    DecisionBuilder decisionBuilder;

    @Before public void setup() {
        decisions = new ArrayList<>();
        decisionBuilder = new DecisionBuilder(decisions);
        Workflow wf = new Workflow("MockWorkflow", "1.0") {
            @Override public void decide(List<Decision> decisions) {
            }
        };
        s1 = new MockAction("s1");
        s2 = new MockAction("s2");
        s3 = new MockAction("s3");
        s4 = new MockAction("s4");
        s5 = new MockAction("s5");
        s6 = new MockAction("s6");
        wf.addActions(s1, s2, s3, s4, s6, s6);
        f1 = () -> s1.withInput("1");
        f2 = () -> s2.withInput("2");
        f3 = () -> s3.withInput("3");
        f4 = () -> s4.withInput("4");
        f5 = () -> s5.withInput("5");
        f6 = () -> s6.withInput("6");
    }

    private void setStates(EventState... states) {
        List<MockAction> actions = asList(s1, s2, s3, s4, s5, s6);
        int i = 0;
        for (EventState state : states) {
            actions.get(i).setEventState(state);
            i++;
        }
    }

    @Test
    public void testSequenceStep1Initial() {
        asList(s1, s2, s3).stream().forEachOrdered((MockAction ma) -> ma.withInput(""));

        decisionBuilder.sequence(f1, f2, f3).decide();
        assertEquals(singletonList(s1.getDecision()), decisions);
    }

    @Test
    public void testSequenceStep1Active() {
        setStates(ACTIVE);
        decisionBuilder.sequence(f1, f2, f3).decide();
        assertEquals(emptyList(), decisions);
    }

    @Test
    public void testSequenceStep2Initial() {
        setStates(SUCCESS);
        decisionBuilder.sequence(f1, f2, f3).decide();
        assertEquals(singletonList(s2.getDecision()), decisions);
    }

    @Test
    public void testSequenceStep2Active() {
        setStates(SUCCESS, ACTIVE);
        decisionBuilder.sequence(f1, f2, f3).decide();
        assertEquals(emptyList(), decisions);
    }

    @Test
    public void testSequenceStep3Initial() {
        setStates(SUCCESS, SUCCESS);
        decisionBuilder.sequence(f1, f2, f3).decide();
        assertEquals(singletonList(s3.getDecision()), decisions);
    }

    @Test
    public void testSequenceStep3Active() {
        setStates(SUCCESS, SUCCESS, ACTIVE);
        decisionBuilder.sequence(f1, f2, f3).decide();
        assertEquals(emptyList(), decisions);
    }

    @Test
    public void testSequenceFinished() {
        setStates(SUCCESS, SUCCESS, SUCCESS);
        decisionBuilder.sequence(f1, f2, f3).decide();
        assertEquals(emptyList(), decisions);
    }

    @Test
    public void testStackStep1() {
        decisionBuilder.sequence(f1).sequence(f2).sequence(f3);
        decisionBuilder.decide();
        assertEquals(singletonList(s1.getDecision()), decisions);
    }

    @Test
    public void testStackStep2() {
        setStates(SUCCESS);
        decisionBuilder.sequence(f1).sequence(f2).sequence(f3);
        decisionBuilder.decide();
        assertEquals(singletonList(s2.getDecision()), decisions);
    }

    @Test
    public void testStackStep3() {
        setStates(SUCCESS, SUCCESS);
        decisionBuilder.sequence(f1).sequence(f2).sequence(f3);
        decisionBuilder.decide();
        assertEquals(singletonList(s3.getDecision()), decisions);
    }

    @Test
    public void testSequenceStepError() {
        s1.setEventState(EventState.ERROR);
        decisionBuilder.sequence(f1, f2, f3).decide();
        assertEquals(emptyList(), decisions);
    }

    @Test
    public void testSplit1() {
        decisionBuilder.split(
//            f1,f2
            decisionBuilder.sequence(f1),
            decisionBuilder.sequence(f2)
        ).sequence(f3)
            .print()
            .decide();
        assertEquals(asList(s1.getDecision(), s2.getDecision()), decisions);
    }


    @Test
    public void testSplit() {

//        List list = asList(f1, asList(asList(f2, f4), asList(f3, f5)), f6);

        decisionBuilder
            .sequence(f1)
            .split(
                f2,
                decisionBuilder.sequence(f3, decisionBuilder.split(f4, decisionBuilder.sequence(f1, f1, f1)))
            )
            .sequence(f6)
            .print();
//            .decide();

        assertEquals(decisions, new ArrayList<Decision>());

    }
}
