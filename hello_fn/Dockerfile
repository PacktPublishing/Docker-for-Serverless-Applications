FROM fnproject/fn-java-fdk:jdk9-1.0.56

WORKDIR /function

COPY ./build/install/hello_fn/lib/*.jar /function/app/

CMD ["com.example.fn.HelloFunction::handleRequest"]
