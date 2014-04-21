package com.clario.swift.example;

import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflow;
import com.amazonaws.services.simpleworkflow.AmazonSimpleWorkflowClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author George Coller
 */
public class Config {
    public static final Logger log = LoggerFactory.getLogger(Config.class);

    private static final Config config = new Config();
    private final ExecutorService service;
    private final AmazonSimpleWorkflow amazonSimpleWorkflow;
    private int poolSize;

    private Config() {
        try {
            Properties p = new Properties();
            p.load(getClass().getClassLoader().getResourceAsStream("config.properties"));
            String id = p.getProperty("amazon.aws.id");
            String key = p.getProperty("amazon.aws.key");
            poolSize = Integer.parseInt(p.getProperty("poolSize"));
            amazonSimpleWorkflow = new AmazonSimpleWorkflowClient(new BasicAWSCredentials(id, key));
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    log.info("Shutting down pool");
                    service.shutdownNow();
                }
            });
            log.info("Starting pool");
            service = Executors.newFixedThreadPool(poolSize);
        } catch (Exception e) {
            throw new IllegalStateException(e.getMessage(), e);
        }
    }

    public static Config getConfig() {
        return config;
    }

    public ExecutorService getService() {
        return service;
    }

    public AmazonSimpleWorkflow getAmazonSimpleWorkflow() {
        return amazonSimpleWorkflow;
    }

    public int getPoolSize() {
        return poolSize;
    }

}
