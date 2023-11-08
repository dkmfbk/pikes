FROM maven:3.9-amazoncorretto-8-debian as builder

RUN apt-get update && apt-get install -y git gnupg2 \
  && gpg --batch --passphrase "pikes" --generate-key <<EOF \
  Key-Type: RSA \
  Key-Length: 2048 \
  Subkey-Type: RSA \
  Subkey-Length: 2048 \
  Name-Real: pikes \
  Name-Email: example@example.com \
  Expire-Date: 0 \
  Passphrase: pikes \
  %commit \
  EOF \
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
  && mvn clean package -DskipTests -Prelease

FROM openjdk:8 as server

COPY . /
COPY --from=builder **/pikes-tintop-1.0-SNAPSHOT-jar-with-dependencies.jar ./pikes-tintop-1.0-SNAPSHOT-jar-with-dependencies.jar

RUN apt-get update && apt-get install -y graphviz

CMD ["sh", "run.sh"]
