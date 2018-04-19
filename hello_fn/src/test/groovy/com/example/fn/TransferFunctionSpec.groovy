package com.example.fn


import com.fnproject.fn.testing.*
import org.junit.*
import spock.lang.Specification
import static org.junit.Assert.*

class TransferFunctionSpec extends Specification {

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

}