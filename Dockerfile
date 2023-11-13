FROM maven:3.9-eclipse-temurin-8-focal as builder

COPY . /
RUN sh ./build.sh
RUN wget https://wordnetcode.princeton.edu/wn3.1.dict.tar.gz
RUN tar -zxvf wn3.1.dict.tar.gz
RUN wget https://git.rwth-aachen.de/coscine/research/pikesdocker/-/archive/main/pikesdocker-main.tar.gz?path=models
RUN tar -zxvf pikesdocker-main.tar.gz?path=models
RUN wget https://git.rwth-aachen.de/coscine/research/pikesdocker/-/archive/main/pikesdocker-main.tar.gz?path=ukb
RUN tar -zxvf pikesdocker-main.tar.gz?path=ukb

FROM openjdk:8 as server

COPY run.sh ./
COPY config-pikes.prop ./
COPY --from=builder pikes-tintop/target/pikes-tintop-1.0-SNAPSHOT-jar-with-dependencies.jar ./pikes-tintop-1.0-SNAPSHOT-jar-with-dependencies.jar
COPY --from=builder dict/* ./wordnet/
COPY --from=builder pikesdocker-main-models/models/* ./models/
COPY --from=builder pikesdocker-main-ukb/ukb/* ./ukb/

RUN apt-get update && apt-get install -y graphviz

CMD ["sh", "run.sh"]
