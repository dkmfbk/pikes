#!/bin/bash
CLASSPATH=pikes-tintop-1.0-SNAPSHOT-jar-with-dependencies.jar
CONF=config-pikes.prop
PORT=8011
RAM=-Xmx8G
java $RAM -cp $CLASSPATH eu.fbk.dkm.pikes.tintop.server.PipelineServer -p $PORT -c $CONF
