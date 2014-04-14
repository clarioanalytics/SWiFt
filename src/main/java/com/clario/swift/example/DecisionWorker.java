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
        p.load(ActivityWorker.class.getResourceAsStream("config.properties"));
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
        DecisionBuilder b = new DecisionBuilder().activity("a.right", "Calc Plus", "1.0").group("a").activity("a.left", "Calc Plus").group("a").activity("b1", "Calc Plus").groupAndParent("b", "a").activity("b2", "Calc Plus").groupAndParent("b", "a").activity("b3", "Calc Plus").groupAndParent("b", "a").activity("c", "Calc Plus").parent("a").mark().activity("d", "Calc Plus");
        poller.addWorkflow("Calculator", "1.0", b.getSteps());
    }

    public static void wireXYZWorkflow(WorkflowPoller poller) {
        DecisionBuilder b = new DecisionBuilder();
        b.activity("first", "Activity X", "1.0");
        b.activity("splitA", "Activity Y", "1.0").group("a");
        b.activity("splitB", "Activity Y").group("a");
        b.activity("join", "Activity X").parent("a");
        b.add(new ActivityDecisionStep("race", "Activity X", "1.0") {
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

        }).parent("a");
        b.mark();
        b.activity("afterMarker", "Activity Z", "1.0");
        ArrayList<DecisionStep> steps = b.getSteps();

        for (DecisionStep it : steps) {
            if (it instanceof ActivityDecisionStep) {
                ((ActivityDecisionStep) it).setScheduleToCloseTimeout(TimeUnit.MINUTES, 1);
            }
        }

        poller.addWorkflow("Demo Workflow", "1.0", steps);
    }
}
