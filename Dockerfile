FROM --platform=linux/arm64 amazoncorretto:21-al2023-jdk
LABEL authors="toni"

EXPOSE 8080 8080

RUN uname -m

RUN yum update && yum install -y glibc
RUN ldd --version


RUN mkdir app

COPY target/rag-sse-1.0-SNAPSHOT.jar app
COPY config.yml app

WORKDIR app

CMD ["java","--add-modules","jdk.incubator.vector","-jar","rag-sse-1.0-SNAPSHOT.jar","server","config.yml"]