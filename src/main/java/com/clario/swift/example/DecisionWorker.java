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

import static com.clario.swift.SwiftUtil.createCancelActivityDecision;


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
            String executionContext = System.getProperty("user.name");
            String pollerId = String.format("decision poller %d", it);

            DecisionPoller poller = new DecisionPoller(pollerId, "dev-clario", "default", executionContext);
            poller.setSwf(new AmazonSimpleWorkflowClient(new BasicAWSCredentials(id, key)));
            wireCalcWorkflow(poller);
            wireDemoWorkflow(poller);
            service.submit(poller);
        }
    }

    public static void wireCalcWorkflow(DecisionPoller poller) {
        WorkflowBuilder b = new WorkflowBuilder("Calculator", "1.0")
            .activity("a.right", "Calc Plus", "1.0")
            .activity("a.left", "Calc Plus", "1.0")
            .activity("b1", "Calc Plus", "1.0")
            .activity("b2", "Calc Plus", "1.0")
            .activity("b3", "Calc Plus", "1.0")
            .withTasks("b.*").addParents("a.*")
            .activity("c", "Calc Plus", "1.0").addParents("b.*")
            .activity("d", "Calc Plus", "1.0").addParents("c");
        poller.addWorkflow(b.buildWorkflow());
    }

    public static void wireDemoWorkflow(DecisionPoller poller) {
        WorkflowBuilder b = new WorkflowBuilder("Demo Workflow", "1.0")
            .activity("first", "Activity X", "1.0")
            .activity("splitA", "Activity Y", "1.0")
            .activity("splitB", "Activity Y", "1.0")
            .activity("join", "Activity X", "1.0")
            .add(new Activity("race", "Activity X", "1.0") {
                @Override
                public List<Decision> decide() {
                    List<Decision> decisions = new ArrayList<>();
                    Task p1 = getParent("splitA");
                    Task p2 = getParent("splitB");
                    if (p1.getState() == TaskState.finish_ok) {
                        Map<String, String> map = new LinkedHashMap<>(1);
                        map.put(getId(), p1.getOutput().get("splitA"));
                        decisions.add(scheduleActivityDecision(new MapSerializer().marshal(map)));
                        decisions.add(createCancelActivityDecision(p2.getId()));
                    } else if (p2.getState() == TaskState.finish_ok) {
                        Map<String, String> map = new LinkedHashMap<>(1);
                        map.put(getId(), p2.getOutput().get("splitB"));
                        decisions.add(scheduleActivityDecision(new MapSerializer().marshal(map)));
                        decisions.add(createCancelActivityDecision(p1.getId()));
                    } else {
                        // Should not reach here if either p1 or p2 finished with an error or cancel;
                        throw new IllegalStateException("Activity race");
                    }
                    return decisions;
                }
            })
            .activity("afterMarker", "Activity Z", "1.0").addParents("join", "race")

            .withTasks("split.*").addParents("first")
            .withTasks("join", "race").addParents("split.*")
            .withTasks(".*").scheduleCloseTimeout(TimeUnit.MINUTES, 1);

        Workflow w = b.buildWorkflow();
        System.out.println(w.toString());
        poller.addWorkflow(w);
    }
}
