package com.clario.swift;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.clario.swift.action.ActionSupplier;
import com.clario.swift.action.MockAction;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import static com.amazonaws.services.simpleworkflow.model.DecisionType.CompleteWorkflowExecution;
import static com.clario.swift.event.EventState.ERROR;
import static com.clario.swift.event.EventState.NOT_STARTED;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author George Coller
 */
public class DecisionBuilderTest {
    private static Logger log = LoggerFactory.getLogger(DecisionBuilderTest.class);

    MockAction s1, s2, s3, s4, s5, s6, s7, s8, s9;
    ActionSupplier f1, f2, f3, f4, f5, f6, f7, f8, f9;
    List<MockAction> mockActions;
    List<Decision> decisions;
    DecisionBuilder builder;

    @Before
    public void setup() {
        decisions = new ArrayList<>();
        builder = new DecisionBuilder(decisions);
        s1 = new MockAction("s1");
        s2 = new MockAction("s2");
        s3 = new MockAction("s3");
        s4 = new MockAction("s4");
        s5 = new MockAction("s5");
        s6 = new MockAction("s6");
        s7 = new MockAction("s7");
        s8 = new MockAction("s8");
        s9 = new MockAction("s9");
        mockActions = asList(s1, s2, s3, s4, s5, s6, s7, s8, s9);

        f1 = () -> s1.withInput("");
        f2 = () -> s2.withInput(s1.getOutput());
        f3 = () -> s3.withInput(s2.getOutput());
        f4 = () -> s4.withInput(s3.getOutput());
        f5 = () -> s5.withInput(s4.getOutput());
        f6 = () -> s6.withInput(s5.getOutput());
        f7 = () -> s7.withInput(s6.getOutput());
        f8 = () -> s8.withInput(s7.getOutput());
        f9 = () -> s9.withInput(s8.getOutput());

    }

    /**
     * Helper class where a list of expected outcomes for each step of the workflow can be replayed.
     */
    class Replay {
        int stepCount;
        List<MockAction> actions = new ArrayList<>();
        List<Map<MockAction, String>> steps = new ArrayList<>();
        final Map<MockAction, String> LAST_STEP = Collections.emptyMap();
        private String completeWorkflowResult;

        Replay() {
            actions.addAll(mockActions);
            addStep();
        }

        DecisionBuilder.DecisionState next(Map<MockAction, String> step) {
            log.info("next " + step);
            if (++stepCount > 1) {
                actions.forEach(MockAction::nextState);
            }
            decisions.clear();
            DecisionBuilder.DecisionState result = builder.decide();

            if (step != LAST_STEP) {
                List<Decision> expectedDecisions = step.keySet().stream()
                                                       .filter(d -> d != null)
                                                       .map(MockAction::getDecision)
                                                       .filter(d -> d != null)
                                                       .collect(toList());
                assertEquals("replay step " + stepCount + " decisions", expectedDecisions, decisions);

                List<String> actualControls = step.keySet().stream()
                                                  .filter(d -> d != null)
                                                  .map(MockAction::getControl)
                                                  .filter(d -> d != null)
                                                  .collect(toList());
                assertEquals("replay step " + stepCount + " controls", step.values().toString(), actualControls.toString());
            }
            return result;
        }

        Replay addStep() {
            steps.add(new HashMap<>());
            return this;
        }

        /**
         * Add an expected control result at the current step
         *
         * @param action action with control value
         * @param expectedControlValue expected control value on action
         */
        Replay expect(MockAction action, String expectedControlValue) {
            steps.get(steps.size() - 1).put(action, expectedControlValue);
            return this;
        }

        /**
         * Perform the test by replaying all the configured steps and expectations.
         */
        void play() {
            steps.add(LAST_STEP);
            DecisionBuilder.DecisionState result = DecisionBuilder.DecisionState.notStarted;
            while (!steps.isEmpty()) {
                result = next(steps.remove(0));
                if (result.isFinished()) {
                    break;
                }
            }

            if (steps.size() > 1) {
                throw new IllegalStateException(steps.size() + " steps were added but not replayed");
            }
            if (!builder.getFailWorkflowDecisionAttributes().isPresent() && result.isError()) {
                throw new IllegalStateException("Result is error but no fail workflow attributes present");
            }

            if (completeWorkflowResult != null) {
                List<Decision> list = builder.findDecisions(CompleteWorkflowExecution);
                assert !list.isEmpty() : "Expecting a CompleteWorkflowExecution decision";
                assert completeWorkflowResult.equals(list.get(0).getCompleteWorkflowExecutionDecisionAttributes().getResult());
            }
        }

        Replay assertCompleteWorkflowResult(String result) {
            this.completeWorkflowResult = result;
            return this;
        }
    }


    @Test
    public void testSingleSequence() {
        builder.sequence(f1, f2, f3);

        new Replay()
            .expect(s1, "s1").addStep()
            .expect(s2, "s1->s2").addStep()
            .expect(s3, "s1->s2->s3")
            .play();
    }

    @Test
    public void testMultipleSequences() {
        builder.sequence(f1).sequence(f2).sequence(f3);
        new Replay()
            .expect(s1, "s1").addStep()
            .expect(s2, "s1->s2").addStep()
            .expect(s3, "s1->s2->s3")
            .play();
    }


    @Test
    public void testIfThenTrue() {
        f3 = () -> s3.withInput(s2.getOutput());
        f4 = () -> s4.withInput(s2.getOutput());
        f5 = () -> s5.withInput("(" + s3.getOutput() + "+" + s4.getOutput() + ")");

        builder
            .sequence(f1)
            .ifThen(() -> true, builder.sequence(f2, builder.split(f3, f4)))
            .sequence(f5);

        new Replay()
            .expect(s1, "s1").addStep()
            .expect(s2, "s1->s2").addStep()
            .expect(s3, "s1->s2->s3")
            .expect(s4, "s1->s2->s4").addStep()
            .expect(s5, "(s1->s2->s3+s1->s2->s4)->s5")
            .play();
    }

    @Test
    public void testIfThenFalse() {
        f5 = () -> s5.withInput(s1.getOutput());

        builder
            .sequence(f1)
            .ifThen(() -> false, builder.sequence(f2, builder.split(f3, f4)))
            .sequence(f5);

        new Replay()
            .expect(s1, "s1").addStep()
            .expect(s5, "s1->s5")
            .play();
    }

    @Test
    public void testSequenceStepError() {
        s2.setEventStates(NOT_STARTED, ERROR);
        builder.sequence(f1, f2, f3);
        new Replay()
            .expect(s1, "s1").addStep()
            .expect(s2, "s1->s2").addStep()
            .expect(s2, "s2:error")
            .play();
    }

    @Test
    public void testFinallyWithError() {
        f4 = () -> s4.withInput("finally");

        s2.setEventStates(NOT_STARTED, ERROR);
        builder.sequence(f1, f2, f3);
        builder.andFinally(f4);

        new Replay()
            .expect(s1, "s1").addStep()
            .expect(s2, "s1->s2").addStep()
            .expect(s4, "finally->s4").addStep()
            .expect(s2, "s2:error").play();
    }

    @Test
    public void testErrorInFinallyBlock() {
        f4 = () -> s4.withInput("finally");

        s4.setEventStates(NOT_STARTED, ERROR);
        builder.sequence(f1, f2);
        builder.andFinally(f4);

        new Replay()
            .expect(s1, "s1").addStep()
            .expect(s2, "s1->s2").addStep()
            .expect(s2, "s1->s2").play();
        
        assertTrue(builder.getFailWorkflowDecisionAttributes().isPresent());
    }


    @Test
    public void testFinallyWithErrorNoFailWorkflowAndNoDecisions() {
        f4 = () -> s4.withInput("finally");

        s2.setEventStates(NOT_STARTED, ERROR);
        s2.withNoFailWorkflowOnError();
        builder.sequence(f1, f2, f3);
        builder.andFinally(f4);

        new Replay()
            .expect(s1, "s1").addStep()
            .expect(s2, "s1->s2").addStep()
            .expect(s4, "finally->s4").addStep()
            .play();
    }

    @Test
    public void testNoFailWorkflowOnError() {
        s2.setEventStates(NOT_STARTED, ERROR);
        s2.withNoFailWorkflowOnError();
        builder.sequence(f1, f2, f3);

        try {
            new Replay()
                .expect(s1, "s1").addStep()
                .expect(s2, "s1->s2").addStep()
                .expect(s2, "error no fail s1->s2")
                .play();
        } catch (IllegalStateException e) {
            assertEquals("Result is error but no fail workflow attributes present", e.getMessage());
        }
    }

    @Test
    public void testFinallyHandleError() {
        s2.setEventStates(NOT_STARTED, ERROR);

        builder.sequence(f1, f2, f3);
        builder.andFinally(() -> {
            builder.getFailWorkflowDecisionAttributes()
                .ifPresent(attrs -> {
                    s4.withInput("handled " + attrs.getReason());
                    builder.removeFailWorkflowExecutionDecisions();
                });
            return s4;
        });

        new Replay()
            .expect(s1, "s1").addStep()
            .expect(s2, "s1->s2").addStep()
            .expect(s4, "handled s2:\nerror->s4")
            .play();
    }


    @Test
    public void testFinallyNoError() {
        f4 = () -> s4.withInput(s3.getOutput());
        f5 = () -> s5.withInput(s3.getOutput());
        builder.sequence(f1, f2, f3);
        builder.andFinally(builder.split(f4, f5));

        new Replay()
            .expect(s1, "s1").addStep()
            .expect(s2, "s1->s2").addStep()
            .expect(s3, "s1->s2->s3").addStep()
            .expect(s4, "s1->s2->s3->s4")
            .expect(s5, "s1->s2->s3->s5")
            .play();
    }

    @Test
    public void testTryCatchWithError() {
        f4 = () -> {
            // demonstrate error decision is available for inspection but won't be retained 
            assert builder.getFailWorkflowDecisionAttributes().isPresent();
            return s4.withInput("");
        };
        f5 = () -> {
            assert builder.getFailWorkflowDecisionAttributes().isPresent();
            return s5.withInput("");
        };
        f6 = () -> s6.withInput(s4.getOutput() + "+" + s5.getOutput());

        s3.setEventStates(NOT_STARTED, ERROR);
        builder.sequence(f1);
        builder.tryCatch(builder.sequence(f2, f3), builder.split(f4, f5));
        builder.sequence(f6);

        new Replay()
            .expect(s1, "s1").addStep()
            .expect(s2, "s1->s2").addStep()
            .expect(s3, "s1->s2->s3").addStep()
            .expect(s4, "s4")
            .expect(s5, "s5").addStep()
            .expect(s6, "s4+s5->s6")
            .play();
    }

    @Test
    public void testTryCatchNoError() {
        f3 = () -> s3.withInput("should never be run");
        f4 = () -> s4.withInput(s2.getOutput());
        builder.tryCatch(builder.sequence(f1, f2), f3);
        builder.sequence(f4);

        new Replay()
            .expect(s1, "s1").addStep()
            .expect(s2, "s1->s2").addStep()
            .expect(s4, "s1->s2->s4")
            .play();

        assertEquals("[{\"TryCatch\":[{\"Seq\":[\"'s1'\",\"'s2'\"]},\"'s3'\"]},{\"Seq\":[\"'s4'\"]}]", builder.toString());
    }

    @Test
    public void testSplitJoin1() {
        f1 = () -> s1.withInput("");
        f2 = () -> s2.withInput("");
        f3 = () -> s3.withInput(s1.getOutput() + "+" + s2.getOutput());
        builder.split(
            builder.sequence(f1),
            builder.sequence(f2)
        ).sequence(f3);

        new Replay()
            .expect(s1, "s1")
            .expect(s2, "s2").addStep()
            .expect(s3, "s1+s2->s3")
            .play();
    }

    @Test
    public void testSplitJoin2() {
        builder.split(
            () -> s1.withInput(""),
            () -> s2.withInput("")
        ).sequence(() -> s3.withInput(s1.getOutput() + "+" + s2.getOutput()));

        new Replay()
            .expect(s1, "s1")
            .expect(s2, "s2").addStep()
            .expect(s3, "s1+s2->s3")
            .play();
    }

    @Test
    public void testWorklflowCompletionDecision() {
        builder
            .sequence(f1, f2)
            .withCompleteWorkflowExecution(() -> s2.getOutput());

        new Replay()
            .expect(s1, "s1").addStep()
            .expect(s2, "s1->s2")
            .assertCompleteWorkflowResult("s1->s2")
            .play();
    }


    @Test
    public void testSequenceSplitSequenceSplitSequence() {
        f1 = () -> s1.withInput("");
        f2 = () -> s2.withInput(s1.getOutput());
        f3 = () -> s3.withInput(s2.getOutput());
        f4 = () -> s4.withInput(s2.getOutput());
        f5 = () -> s5.withInput(s4.getOutput());
        f6 = () -> s6.withInput(s4.getOutput());
        f7 = () -> s7.withInput("(" + s5.getOutput() + ")+(" + s6.getOutput() + ")");
        f8 = () -> s8.withInput(s2.getOutput());
        f9 = () -> s9.withInput("(" + s3.getOutput() + ")+(" + s7.getOutput() + ")+(" + s8.getOutput() + ")");

        builder
            .sequence(f1, f2)
            .split(
                f3,
                builder.sequence(f4, builder.split(f5, f6), f7),
                f8
            )
            .sequence(f9);

        new Replay()
            .expect(s1, "s1").addStep()
            .expect(s2, "s1->s2").addStep()
            .expect(s3, "s1->s2->s3")
            .expect(s4, "s1->s2->s4")
            .expect(s8, "s1->s2->s8").addStep()

            .expect(s5, "s1->s2->s4->s5")
            .expect(s6, "s1->s2->s4->s6").addStep()

            .expect(s7, "(s1->s2->s4->s5)+(s1->s2->s4->s6)->s7").addStep()
            .expect(s9, "(s1->s2->s3)+((s1->s2->s4->s5)+(s1->s2->s4->s6)->s7)+(s1->s2->s8)->s9")
            .play();
    }
}
