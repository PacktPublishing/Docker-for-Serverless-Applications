package com.example.fn


import com.fnproject.fn.testing.*
import org.junit.*
import spock.lang.Specification
import static org.junit.Assert.*

class TransferFunctionSpec extends Specification {

    /*
    @Rule
    final FnTestingRule fn = FnTestingRule.createDefault();

    def "should return greeting"() {

    	given:
        fn.givenEvent().withBody(input).enqueue()

        when:
        fn.thenRun(HelloFunction.class, "handleRequest")

        then:
        fn.onlyResult.bodyAsString == result

        where:
        input   | result
        "test"  | "Hello, test!"
        "world" | "Hello, world!"
        ""      | "Hello, world!"

    }
    */

    /*
    def "should call faas's hivectl"() {
        when:
        def result = new TransferFunction().faasAdjust("1", "55700", -10)

        then:
        result == true
    }
    */

    def "should call whisk's accountctl"() {
        when:
        def result = new TransferFunction().whiskAdjust("1", "AB123", -10)

        then:
        result == true
    }

    /*
    def "should find tel no +661234567"() {
        when:
        def result = new TransferFunction().lookup("+661234567")

        then:
        result.bankName == "faas"
        result.accountId == "55700"
    }

    def "should find tel no +661111111"() {
        when:
        def result = new TransferFunction().lookup("+661111111")

        then:
        result.bankName == "whisk"
        result.accountId == "A1234"
    }

    def "should not find tel no +660000000"() {
        when:
        def result = new TransferFunction().lookup("+660000000")

        then:
        result == null
    }
    */

}