FROM maven:3.9-eclipse-temurin-8-focal as builder

COPY . /
RUN sh ./build.sh

FROM openjdk:8 as server

COPY . /
COPY --from=builder **/pikes-tintop-1.0-SNAPSHOT-jar-with-dependencies.jar ./pikes-tintop-1.0-SNAPSHOT-jar-with-dependencies.jar

RUN apt-get update && apt-get install -y graphviz

CMD ["sh", "run.sh"]
