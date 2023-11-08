FROM openjdk:8

COPY . /

RUN apt-get update && apt-get install -y graphviz

CMD ["sh", "run.sh"]
