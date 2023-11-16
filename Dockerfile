FROM maven:latest as BUILDER
WORKDIR /app
ADD . .
RUN mvn install -Dmaven.test.skip=true

FROM tomcat:9.0.81-jdk8

COPY catalina.properties /usr/local/tomcat/conf/
COPY --from=BUILDER app/target/pushservice-0.0.1.war /usr/local/tomcat/webapps/pushservice.war