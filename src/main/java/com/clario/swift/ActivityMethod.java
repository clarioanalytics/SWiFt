package com.clario.swift;

import com.amazonaws.services.simpleworkflow.model.RegisterActivityTypeRequest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark any method as an activity method that can handle a registered SWF Activity Tasks.
 *
 * @author George Coller
 * @see RegisterActivityTypeRequest
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
public @interface ActivityMethod {
    /**
     * Registered activity name.
     *
     * @see RegisterActivityTypeRequest#name
     */
    String name();

    /**
     * Registered activity version.
     *
     * @see RegisterActivityTypeRequest#version
     */
    String version();

    /**
     * Override activity's default schedule to close timeout.
     * <pre>
     * schedule ---> start ---> close
     * |_____________________________|
     * </pre>
     *
     * @see RegisterActivityTypeRequest#defaultTaskScheduleToCloseTimeout
     */
    String scheduleToCloseTimeout() default "";

    /**
     * Override activity's default schedule to close timeout.
     * <pre>
     * schedule ---> start ---> close
     * |_________________|
     * </pre>
     *
     * @see RegisterActivityTypeRequest#defaultTaskScheduleToStartTimeout
     */
    String scheduleToStartTimeout() default "";

    /**
     * Override activity's default schedule to close timeout.
     * <pre>
     * schedule ---> start ---> close
     *              |_______________|
     * </pre>
     *
     * @see RegisterActivityTypeRequest#defaultTaskStartToCloseTimeout
     */
    String startToCloseTimeout() default "";


    /**
     * Override the activity's default heartbeat timeout.
     *
     * @see RegisterActivityTypeRequest#defaultTaskHeartbeatTimeout
     */
    String heartbeatTimeout() default "";
}