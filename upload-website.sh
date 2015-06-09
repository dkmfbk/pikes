#!/bin/bash

# set -e

mvn clean package site:site site:deploy -DskipTests -Dsite.root=http://pikes.fbk.eu/
git checkout gh-pages
cp -r target/website/* .
git add *
git commit -a -m "Website"
git push
git checkout master
