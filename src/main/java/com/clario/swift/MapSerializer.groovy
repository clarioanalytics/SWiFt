package com.clario.swift

import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import groovy.util.logging.Slf4j

/**
 * Serialize to/from a Map&lt;String,String&gt;.
 * @author George Coller
 */
@Slf4j
public class MapSerializer {

    Map<String, String> unmarshal(String value) {
        try {
            def map = new JsonSlurper().parseText(value)
            map;
        } catch (e) {
            log.error("Failed to convert map with: $value", e)
            throw e
        }
    }

    String marshal(String key, String value) {
        marshal([(key): value])
    }

    String marshal(Map<String, String> map) {
        JsonOutput.toJson(map)
    }
}