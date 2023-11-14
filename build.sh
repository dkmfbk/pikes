git clone https://github.com/fbk/utils \
  && cd utils \
  && mvn versions:set -DnewVersion=3.2-SNAPSHOT \
  && mvn clean install -DskipTests -Dgpg.skip \
  && cd .. \
  && git clone https://github.com/fbk/parent \
  && cd parent \
  && git checkout develop \
  && mvn versions:set -DnewVersion=2.3-SNAPSHOT \
  && mvn clean install -DskipTests -Dgpg.skip \
  && cd .. \
  && git clone https://github.com/dkmfbk/rdfpro parent/rdfpro \
  && cd parent/rdfpro \
  && git checkout develop \
  && mvn versions:set -DnewVersion=0.7-SNAPSHOT \
  && mvn clean install -DskipTests -Dgpg.skip \
  && cd ../.. \
  && git clone https://github.com/fbk/fcw \
  && cd fcw \
  && grep -rl "http://model.dbpedia-spotlight.org/en" . | xargs sed -i 's/http:\/\/model.dbpedia-spotlight.org\/en/https:\/\/api.dbpedia-spotlight.org\/en/g' \
  && git checkout develop \
  && mvn versions:set -DnewVersion=1.0-SNAPSHOT \
  && mvn clean install -DskipTests -Dgpg.skip \
  && cd .. \
  && git clone https://github.com/dhfbk/tint \
  && cd tint \
  && mvn clean install -DskipTests -Dgpg.skip \
  && cd .. \
  && mvn clean package -DskipTests -Dgpg.skip -Prelease
