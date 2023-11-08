apt-get update && apt-get install -y git

git clone https://github.com/fbk/utils \
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
