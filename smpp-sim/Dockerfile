FROM eclipse-temurin:17-jdk-jammy

RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*

WORKDIR /opt/smpp

RUN curl -fsSL -o jsmpp.jar          https://repo1.maven.org/maven2/org/jsmpp/jsmpp/3.0.1/jsmpp-3.0.1.jar && \
    curl -fsSL -o jsmpp-examples.jar https://repo1.maven.org/maven2/org/jsmpp/jsmpp-examples/3.0.1/jsmpp-examples-3.0.1.jar && \
    curl -fsSL -o slf4j-api.jar      https://repo1.maven.org/maven2/org/slf4j/slf4j-api/2.0.13/slf4j-api-2.0.13.jar && \
    curl -fsSL -o slf4j-simple.jar   https://repo1.maven.org/maven2/org/slf4j/slf4j-simple/2.0.13/slf4j-simple-2.0.13.jar

EXPOSE 2775

CMD ["java", "-cp", "jsmpp.jar:jsmpp-examples.jar:slf4j-api.jar:slf4j-simple.jar", \
     "org.jsmpp.examples.SMPPServerSimulator"]