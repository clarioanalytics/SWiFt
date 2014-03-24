package com.clario.swift.example

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient
import com.amazonaws.services.simpleworkflow.model.Decision
import com.clario.swift.ActivityDecisionStep
import com.clario.swift.DecisionBuilder
import com.clario.swift.MapSerializer
import com.clario.swift.WorkflowPoller
import groovy.util.logging.Slf4j

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * @author George Coller
 */
@Slf4j
public class DecisionWorker {

    public static void main(String[] args) {
        Properties p = new Properties()
        p.load(ActivityWorker.class.getResourceAsStream("config.properties"))
        String id = p.getProperty("amazon.aws.id")
        String key = p.getProperty("amazon.aws.key")
        int threads = Integer.parseInt(p.getProperty("activity.threads"))
        ExecutorService service = Executors.newFixedThreadPool(threads)

        (1..threads).each {
            WorkflowPoller poller = new WorkflowPoller("decision poller $it")
            poller.domain = "dev-clario"
            poller.taskList = "default"
            poller.swf = new AmazonSimpleWorkflowClient(new BasicAWSCredentials(id, key))
            poller.executionContext = System.getProperty("user.name")
            wireCalcWorkflow(poller)
            wireXYZWorkflow(poller)
            service.submit(poller)
        }
    }

    def static void wireCalcWorkflow(WorkflowPoller poller) {
        def b = new DecisionBuilder()
        b.peers {
            activity('a.right', 'Calc Plus', '1.0')
            activity('a.left', 'Calc Plus')
        } {
            peers {
                activity('b1', 'Calc Plus')
                activity('b2', 'Calc Plus')
                activity('b3', 'Calc Plus')
            } {
                activity('c', 'Calc Plus')
            }
        }.decisionGroup {
            activity('d', 'Calc Plus')
        }
        poller.addWorkflow('Calculator', '1.0', b.steps)
    }

    static void wireXYZWorkflow(WorkflowPoller poller) {
        DecisionBuilder b = new DecisionBuilder()
        b.activity('first', 'Activity X', '1.0') {
            peers {
                activity('splitA', 'Activity Y', '1.0')
                activity('splitB', 'Activity Y')
            } {
                activity('join', 'Activity X')
                addStep(
                        new ActivityDecisionStep('race', 'Activity X', "1.0") {
                            // Example of starting race with first parent to complete, canceling other parent
                            @Override
                            List<Decision> decide() {
                                def decisions = []
                                def p1 = getParent("splitA") as ActivityDecisionStep
                                def p2 = getParent("splitB") as ActivityDecisionStep
                                // Note: Groovy weirdness 'this' refers to DecisionWorker, not this new subclass,
                                // so need to call getStepId() property explicitly
                                if (p1.stepFinished) {
                                    def map = [(getStepId()): p1.output['splitA']]
                                    def out = new MapSerializer().marshal(map)
                                    decisions << createScheduleActivityDecision(out)
                                    decisions << p2.createCancelActivityDecision()
                                } else if (p2.stepFinished) {
                                    def map = [(getStepId()): p2.output['splitB']]
                                    def out = new MapSerializer().marshal(map)
                                    decisions << createScheduleActivityDecision(out)
                                    decisions << p1.createCancelActivityDecision()
                                }
                                decisions
                            }
                        }
                )
            }.decisionGroup {
                activity('afterMarker', 'Activity Z', '1.0')
            }
        }

        def steps = b.getSteps()
        steps.findAll {
            it instanceof ActivityDecisionStep
        }.each { ActivityDecisionStep step ->
            step.setScheduleToCloseTimeout(TimeUnit.MINUTES, 1)
        }
        poller.addWorkflow("Demo Workflow", "1.0", steps)
    }
}