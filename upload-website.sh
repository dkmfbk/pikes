#!/bin/bash

CHANGED=$(git diff-index --name-only HEAD --)
if [ -n "$CHANGED" ]; then
    echo "There are uncommitted changes on GIT. Commit them before releasing website."
    exit
fi

mvn clean site:site site:deploy -DskipTests -Dsite.root=http://pikes.fbk.eu/
git checkout gh-pages
yes | cp -r target/website/* .
git add *
git commit -a -m "Website"
git push
git checkout master
