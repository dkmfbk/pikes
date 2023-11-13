FROM maven:3.9-eclipse-temurin-8-focal as builder

COPY . /
RUN apt-get update && apt-get install -y git
RUN sh ./build.sh
RUN sh ./getDependencies.sh

FROM openjdk:8 as server

COPY run.sh ./
COPY config-pikes.prop ./
COPY --from=builder pikes-tintop/target/pikes-tintop-1.0-SNAPSHOT-jar-with-dependencies.jar ./pikes-tintop-1.0-SNAPSHOT-jar-with-dependencies.jar
COPY --from=builder wordnet ./wordnet/
COPY --from=builder models ./models/
COPY --from=builder ukb ./ukb/

RUN apt-get update && apt-get install -y graphviz && chmod 777 ukb

CMD ["sudo", "sh", "run.sh"]
