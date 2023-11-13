apt-get update && apt-get install -y git

git clone https://github.com/fbk/utils \
  && cd utils \
  && mvn clean install -DskipTests -Dgpg.skip \
  && cd .. \
  && git clone https://github.com/dkmfbk/rdfpro \
  && cd rdfpro \
  && mvn clean install -DskipTests -Dgpg.skip \
  && cd .. \
  && git clone https://github.com/fbk/fcw \
  && cd fcw \
  && git checkout develop \
  && mvn clean install -DskipTests -Dgpg.skip \
  && cd .. \
  && git clone https://github.com/dhfbk/tint \
  && cd tint \
  && mvn clean install -DskipTests -Dgpg.skip \
  && cd .. \
  && mvn clean package -DskipTests -Dgpg.skip -Prelease
