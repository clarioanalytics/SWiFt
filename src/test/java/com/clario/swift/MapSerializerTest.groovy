package com.clario.swift

import spock.lang.Specification

/**
 * @author George Coller
 */
public class MapSerializerTest extends Specification {
    def serializedString = '{"":"0","a":"1","b":"","c":"2","d":null,"e":"3","f|f":"4|4"}'


    void 'Test Unmarshal'() {
        when:
        Map<String, String> map = new MapSerializer().unmarshal(serializedString)
        then:
        assert map.size() == 7
        assert map[''] == '0'
        assert map['a'] == '1'
        assert map['b'] == ''
        assert map['c'] == '2'
        assert map['d'] == null
        assert map['e'] == '3'
        assert map['f|f'] == '4|4'
    }

    void 'Test Marshal'() {
        when:
        def map = ["": "0", "a": "1", "b": "", "c": "2", "d": null, "e": "3", "f|f": "4|4"]
        def value = new MapSerializer().marshal(map)
        then:
        assert value == serializedString
    }
}