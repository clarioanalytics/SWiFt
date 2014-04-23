package com.clario.swift;

import com.amazonaws.services.simpleworkflow.model.RegisterActivityTypeRequest;
import com.amazonaws.services.simpleworkflow.model.RespondActivityTaskCompletedRequest;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Mark a method as an activity method that can handle a registered SWF Activity Tasks.
 * Methods annotated with <code>ActivityMethod</code> must have the following signature:
 * <pre>
 * &#64;ActivityMethod(name="doSomething", version="1.0")
 * Object methodName({@link ActivityContext} context) {...}
 *
 * or
 *
 * &#64;ActivityMethod(name="doSomething", version="1.0")
 * void methodName({@link ActivityContext} context) {...}
 * </pre>
 * <p/>
 * If the return type is void or the method returns null an empty string will be recorded as the activity task result.
 * Otherwise the result will be recorded as the return value converted to a string using {@link #toString}.
 * <p/>
 * Activities that throw exceptions will be recorded as an error on their related workflow.
 *
 * @author George Coller
 * @see ActivityContext
 * @see RegisterActivityTypeRequest for details and limits of the parameters.
 * @see RespondActivityTaskCompletedRequest#result
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
     * Description of activity.
     *
     * @see RegisterActivityTypeRequest#description
     */
    String description() default "";

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