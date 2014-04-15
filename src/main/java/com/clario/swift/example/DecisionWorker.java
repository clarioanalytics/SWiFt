package com.clario.swift.example;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient;
import com.amazonaws.services.simpleworkflow.model.Decision;
import com.clario.swift.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;


/**
 * @author George Coller
 */
public class DecisionWorker {
    public static final Logger log = LoggerFactory.getLogger(ActivityWorker.class);

    public static void main(String[] args) throws IOException {
        Properties p = new Properties();
        p.load(DecisionWorker.class.getClassLoader().getResourceAsStream("config.properties"));
        final String id = p.getProperty("amazon.aws.id");
        final String key = p.getProperty("amazon.aws.key");
        int threads = Integer.parseInt(p.getProperty("activity.threads"));
        final ExecutorService service = Executors.newFixedThreadPool(threads);

        for (int it = 1; it <= threads; it++) {
            WorkflowPoller poller = new WorkflowPoller(String.format("decision poller %d", it));
            poller.setDomain("dev-clario");
            poller.setTaskList("default");
            poller.setSwf(new AmazonSimpleWorkflowClient(new BasicAWSCredentials(id, key)));
            poller.setExecutionContext(System.getProperty("user.name"));
            wireCalcWorkflow(poller);
            wireXYZWorkflow(poller);
            service.submit(poller);
        }
    }

    public static void wireCalcWorkflow(WorkflowPoller poller) {
        DecisionBuilder b = new DecisionBuilder()
            .activity("a.right", "Calc Plus", "1.0")
            .activity("a.left", "Calc Plus")
            .activity("b1", "Calc Plus")
            .activity("b2", "Calc Plus")
            .activity("b3", "Calc Plus")
            .withEach("b.*").addParents("a.*")
            .activity("c", "Calc Plus").addParents("b.*")
            .mark()
            .activity("d", "Calc Plus");
        poller.addWorkflow("Calculator", "1.0", b.buildStepList());
    }

    public static void wireXYZWorkflow(WorkflowPoller poller) {
        DecisionBuilder b = new DecisionBuilder()
            .activity("first", "Activity X", "1.0")
            .activity("splitA", "Activity Y", "1.0")
            .activity("splitB", "Activity Y")

            .activity("join", "Activity X")
            .add(new ActivityDecisionStep("race", "Activity X", "1.0") {
                @Override
                public List<Decision> decide() {
                    List<Decision> decisions = new ArrayList<>();
                    DecisionStep p1 = getParent("splitA");
                    DecisionStep p2 = getParent("splitB");
                    // Note: Groovy weirdness 'this' refers to DecisionWorker, not this new subclass,
                    // so need to call getStepId() property explicitly
                    if (p1.isStepFinished()) {
                        Map<String, String> map = new LinkedHashMap<>(1);
                        map.put(getStepId(), p1.getOutput().get("splitA"));
                        decisions.add(createScheduleActivityDecision(new MapSerializer().marshal(map)));
                        decisions.add(((ActivityDecisionStep) p2).createCancelActivityDecision());
                    } else if (p2.isStepFinished()) {
                        Map<String, String> map = new LinkedHashMap<>(1);
                        map.put(getStepId(), p2.getOutput().get("splitB"));
                        decisions.add(createScheduleActivityDecision(new MapSerializer().marshal(map)));
                        decisions.add(((ActivityDecisionStep) p1).createCancelActivityDecision());
                    }

                    return decisions;
                }
            })
            .withEach("join", "race").addParents("split.*");

        b.mark();
        b.activity("afterMarker", "Activity Z", "1.0");

        ArrayList<DecisionStep> steps = b.buildStepList();

        for (DecisionStep it : steps) {
            if (it instanceof ActivityDecisionStep) {
                ((ActivityDecisionStep) it).setScheduleToCloseTimeout(TimeUnit.MINUTES, 1);
            }
        }

        poller.addWorkflow("Demo Workflow", "1.0", steps);
    }
}
