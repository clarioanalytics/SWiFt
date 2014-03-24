package com.clario.swift

import spock.lang.Specification

import java.util.concurrent.TimeUnit

/**
 * @author George Coller
 */
public class DecisionBuilderTest extends Specification {


    def 'test builder with groups and workflowGroup'() {
        when:
        def b = new DecisionBuilder();
        b.activity('a1', 'Act 1', '1.0')
        b.peers {
            activity('a2', 'Act 1', '1.0')
            activity('a3', 'Act 2', '2.0')
        } {
            peers {
                activity('b1', 'Act 1').retry(3, TimeUnit.SECONDS.toMillis(5))
                activity('b2', 'Act 2')
                activity('b3', 'Act 3', '1.0')
            } {
                activity('c', 'Act 1', '2.0')
            }
        } decisionGroup {
            activity('d', 'Act 1')
        }
        b.activity('f', 'Act 3') {
            activity('g', 'Act 1')
        }

        then:
        assert b.toString() == '''
a1 'Act 1' '1.0' children(DecisionGroup:1)
a2 'Act 1' '1.0' children(b1, b2, b3)
a3 'Act 2' '2.0' children(b1, b2, b3)
b1 'Act 1' '1.0' children(c) parents(a2, a3)
b2 'Act 2' '2.0' children(c) parents(a2, a3)
b3 'Act 3' '1.0' children(c) parents(a2, a3)
c 'Act 1' '2.0' children(DecisionGroup:1) parents(b1, b2, b3)
DecisionGroup:1 children(d, f) parents(a1, c)
d 'Act 1' '2.0' parents(DecisionGroup:1)
f 'Act 3' '1.0' children(g) parents(DecisionGroup:1)
g 'Act 1' '2.0' parents(f)
        '''.stripIndent().trim()

        b.steps.each {
            assert null == it.parents.find { step -> step instanceof DecisionBuilder.PeerStep }
            assert null == it.children.find { step -> step instanceof DecisionBuilder.PeerStep }
        }
    }

}