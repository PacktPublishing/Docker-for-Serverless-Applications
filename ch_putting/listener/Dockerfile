FROM openjdk:8u151-jdk-alpine

RUN  mkdir /app
COPY ./build/install/listener/lib/*.jar /app/

ENV BLOCKCHAIN_SERVICE http://blockchain:8545/

WORKDIR /app
CMD ["java", "-cp", "*", "event.listener.Main"]
