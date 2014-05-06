package com.clario.swift.examples;

import com.amazonaws.services.simpleworkflow.model.Decision;
import com.clario.swift.Workflow;

import java.util.List;
import java.util.concurrent.TimeUnit;

import static com.clario.swift.SwiftUtil.readFile;
import static com.clario.swift.examples.Config.submit;

/**
 * @author George Coller
 */
public class WorkflowStarter extends Workflow {
    public static final String STARTER_DOMAIN = "dev-clario";
    public static final String STARTER_TASKLIST = "services-api";
    public static final String STARTER_WF_NAME = "API_Index_Document";
    public static final String STARTER_WF_VERSION = "1";

    public WorkflowStarter() {
        super(STARTER_WF_NAME, STARTER_WF_VERSION);
    }

    public static void main(String[] args) {
        Workflow workflow = new WorkflowStarter()
            .withDomain(STARTER_DOMAIN)
            .withTaskList(STARTER_TASKLIST)
            .withExecutionStartToCloseTimeout(TimeUnit.HOURS, 1)
            .withTaskStartToCloseTimeout(TimeUnit.MINUTES, 30);
        submit(workflow, readFile(WorkflowStarter.class, "WorkflowStarterInput.txt"));
    }

    @Override
    public void decide(List<Decision> decisions) {
        // do nothing
    }
}
