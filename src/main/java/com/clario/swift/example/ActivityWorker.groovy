package com.clario.swift.example

import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient
import com.clario.swift.ActivityContext
import com.clario.swift.ActivityMethod
import com.clario.swift.ActivityPoller
import groovy.util.logging.Slf4j

import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * @author George Coller
 */
@Slf4j
public class ActivityWorker {

    public static void main(String[] args) {
        Properties p = new Properties()
        p.load(ActivityWorker.class.getResourceAsStream("config.properties"))
        String id = p.getProperty("amazon.aws.id")
        String key = p.getProperty("amazon.aws.key")
        int threads = Integer.parseInt(p.getProperty("activity.threads"))
        ExecutorService service = Executors.newFixedThreadPool(threads)

        (1..threads).each {
            ActivityPoller poller = new ActivityPoller("activity poller $it")
            poller.domain = "dev-clario"
            poller.taskList = "default"
            poller.swf = new AmazonSimpleWorkflowClient(new BasicAWSCredentials(id, key))
            poller.addActivities(new ActivityWorker())
            service.submit(poller)
        }
    }

    @ActivityMethod(name = "Calc Plus", version = "1.0")
    void calcPlus(ActivityContext context) {
        int sum = unmarshalInput(context.inputs)
        context.setOutput("$sum")
    }

    @ActivityMethod(name = "Activity X", version = "1.0")
    void activityX(ActivityContext context) {
        int i = unmarshalInput(context.inputs)
        sleep(TimeUnit.SECONDS.toMillis(2))
        context.setOutput("${i + 1}")
    }

    @ActivityMethod(name = "Activity Y", version = "1.0")
    void activityY(ActivityContext context) {
        int i = unmarshalInput(context.inputs)
        sleep(TimeUnit.SECONDS.toMillis(2))
        context.setOutput("${i + 100}")
    }


    @ActivityMethod(name = "Activity Z", version = "1.0")
    void activityZ(ActivityContext context) {
        int i = unmarshalInput(context.inputs)
        sleep(TimeUnit.SECONDS.toMillis(5))
        log.info("Activity Z ${context.stepId}: record heartbeat")
        context.recordHeartbeat("some deets: ${new Date()}")
        sleep(TimeUnit.SECONDS.toMillis(10))
        context.setOutput("${i + 1000}")
    }

    static def int unmarshalInput(Map<String, String> map) {
        def integers = map.values().collect { Integer.parseInt(it) }
        integers.sum() as Integer
    }
}