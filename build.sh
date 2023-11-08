apt-get update && apt-get install -y git gnupg
gpg --batch --passphrase "pikes" --generate-key <<EOF
Key-Type: RSA
Key-Length: 2048
Subkey-Type: RSA
Subkey-Length: 2048
Name-Real: pikes
Name-Email: example@example.com
Expire-Date: 0
Passphrase: pikes
%commit
EOF

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
