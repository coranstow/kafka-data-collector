#!/bin/sh

OUTPUT=output1.json
rm $OUTPUT
java -cp ../kafka-config-collector/build/libs/kafka-config-collector-0.1.jar io.confluent.ps.tools.GetKafkaClusterData -c client.properties --bootstrap-servers asw-kafka-a1.lxd:9091 -o $OUTPUT
