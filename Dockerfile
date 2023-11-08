FROM maven:3.9-amazoncorretto-8-debian as builder

RUN apt-get install -y git \
  && git clone https://github.com/fbk/utils \
  && cd utils \
  && mvn clean install \
  && cd .. \
  && git clone https://github.com/fbk/fcw \
  && cd fcw \
  && git checkout develop \
  && mvn clean install \
  && cd .. \
  && git clone https://github.com/dhfbk/tint \
  && cd tint \
  && git checkout develop \
  && mvn clean install \
  && cd .. \
  && git clone https://github.com/dkmfbk/pikes \
  && cd pikes \
  && git checkout develop \
  && mvn clean package -DskipTests -Prelease

FROM openjdk:8 as server

COPY . /
COPY --from=builder **/pikes-tintop-1.0-SNAPSHOT-jar-with-dependencies.jar ./pikes-tintop-1.0-SNAPSHOT-jar-with-dependencies.jar

RUN apt-get update && apt-get install -y graphviz

CMD ["sh", "run.sh"]
