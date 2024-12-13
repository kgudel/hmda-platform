FROM eclipse-temurin:23.0.1_11-jdk-alpine

RUN apk upgrade --update-cache --available && apk add openssl
RUN adduser -H -S -G root -h /opt hmda
RUN mkdir /opt/docker
RUN mkdir /opt/docker/certs
RUN mkdir /opt/docker/certs/tls-cert
RUN mkdir /opt/docker/certs/tls-key
RUN mkdir /opt/docker/certs/ca-cert
COPY ./ca.crt /opt/docker/certs/ca-cert/ca-crt
COPY ./tls.crt /opt/docker/certs/tls-cert/tls-crt
COPY ./tls.key /opt/docker/certs/tls-key/tls-key
COPY ./cacerts /opt/java/openjdk/lib/security/cacerts
COPY ./server.p12 /opt/docker/server.p12
RUN chmod -R 777 /opt/*
RUN chown -R hmda /opt/*

USER hmda