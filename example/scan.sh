#!/bin/sh

java -cp ../kafka-config-collector/build/libs/kafka-config-collector-0.1.jar io.confluent.ps.tools.TopicConfigScanner output1.json scan-config.properties
