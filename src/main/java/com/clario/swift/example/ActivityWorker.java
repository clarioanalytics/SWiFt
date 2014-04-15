package com.clario.swift.example;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient;
import com.clario.swift.ActivityContext;
import com.clario.swift.ActivityMethod;
import com.clario.swift.ActivityPoller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static java.lang.String.valueOf;

/**
 * @author George Coller
 */
public class ActivityWorker {
    public static final Logger log = LoggerFactory.getLogger(ActivityWorker.class);

    public static void main(String[] args) throws IOException {
        Properties p = new Properties();
        p.load(ActivityWorker.class.getClassLoader().getResourceAsStream("config.properties"));
        String id = p.getProperty("amazon.aws.id");
        String key = p.getProperty("amazon.aws.key");
        int threads = Integer.parseInt(p.getProperty("activity.threads"));
        ExecutorService service = Executors.newFixedThreadPool(threads);

        for (int it = 1; it <= threads; it++) {
            ActivityPoller poller = new ActivityPoller(String.format("activity poller %s", it));
            poller.setDomain("dev-clario");
            poller.setTaskList("default");
            poller.setSwf(new AmazonSimpleWorkflowClient(new BasicAWSCredentials(id, key)));
            poller.addActivities(new ActivityWorker());
            service.submit(poller);
        }
    }

    @ActivityMethod(name = "Calc Plus", version = "1.0")
    public void calcPlus(ActivityContext context) {
        int sum = unmarshalInput(context.getInputs());
        context.setOutput(valueOf(sum));
    }

    @ActivityMethod(name = "Activity X", version = "1.0")
    public void activityX(ActivityContext context) {
        final int i = unmarshalInput(context.getInputs());
        sleep(TimeUnit.SECONDS.toMillis(2));
        context.setOutput(valueOf(i + 1));
    }

    @ActivityMethod(name = "Activity Y", version = "1.0")
    public void activityY(ActivityContext context) {
        final int i = unmarshalInput(context.getInputs());
        sleep(TimeUnit.SECONDS.toMillis(2));
        context.setOutput(valueOf(i + 100));
    }

    @ActivityMethod(name = "Activity Z", version = "1.0")
    public void activityZ(final ActivityContext context) {
        final int i = unmarshalInput(context.getInputs());
        sleep(TimeUnit.SECONDS.toMillis(5));
        log.info("Activity Z " + context.getStepId() + ": record heartbeat");
        context.recordHeartbeat("some deets: " + valueOf(new Date()));
        sleep(TimeUnit.SECONDS.toMillis(10));
        context.setOutput(valueOf(i + 1000));
    }

    public static int unmarshalInput(Map<String, String> map) {
        int sum = 0;
        for (String s : map.values()) {
            sum += Integer.parseInt(s);
        }
        return sum;
    }

    private static void sleep(long time) {
        try {
            Thread.sleep(time);
        } catch (InterruptedException e) {
            throw new IllegalStateException("Interrupted", e);
        }
    }
}
