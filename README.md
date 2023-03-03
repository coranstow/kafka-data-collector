# kafka-data-collector
Tools to assist with automated collection of information about a Kafka cluster.

# Goal

This tool dumps your kafka configuration, most importantly, the topic configuration data for health analysing.

This tool also ships with a Common Set of checks that you may want to perform on the output of the data collected.

# Building

```bash
$ ./gradlew clean jar
```

You may find the built jar file located at `kafka-data-collector/build/libs`

# Running

## Getting all your kafka configuration as a JSON file
See `example/collect-data.sh`

You might need to edit the client.properties to reflect your actual kafka client configuration.

You will have to specify the bootstrap server as well.

You can see `example/output.json` for an example output file for your reference. 

The file is self-explanatory.

## Analysing your kafka cluster configuration
See `example/scan.sh`.

You might have to specify the collected datafile as a JSON input.

### Configuration of your scanner
See `example/scan-config.properties`

You can define multiple scan sets by `scan.list` and a bunch of parameters under `setx.xxx` settings.

Each set can be enabled/disabled by setting `set1.enabled` = `true|false`

Each set may define a few filters:
```
set1.topics.whitelist.regex.0=.*
set1.topics.whitelist.regex.1=.*
set1.topics.whitelist.regex.2=.*
set1.topics.include.1=wow  
set1.topics.exclude.0=wows 
set1.topics.blacklist.regex.0=^656_.*$
```
Note filters are executed in this way:

- If a topic name matches any of the blacklist regular expression, it will not be checked
- If a topic name matches any of the exclude list by string equality, it will not be checked
- If a topic name matches any of the include list by string equality, it will be checked
- If a topic name matches any of the whitelist regular expression, it will be checked
- By default, the topic will not be checked

You can define a task (check) by specifing the class name
`set1.task.0=io.confluent.ps.tools.checks.OfflinePartitionCheck`

You can set parameters to this check task by adding config prefixes.
`set1.task.0.config-key=config-value`
`set1.task.0.config-key-other=config-value-other`

All these configs will be passed to the task as a properties as:
```json
{ 
   "config-key": "config-value",
   "config-key-other": "config-value-other"
}
```

You may implement additional checks by implementing `io.confluent.ps.tools.checks.ConfigCheckTask`.

For example or reference, please refer to `kafka-data-collector/src/main/java/io/confluent/ps/tools/checks` package.

The scan result will report the offended rules using the notifier specified by
```
notification.class=io.confluent.ps.tools.notifications.DefaultNotifier
notification.output.filename.template=scan-result-%TS%.txt
```
The default config (above) writes the summary to a file.

```
notification.class=io.confluent.ps.tools.notifications.SMTPNotifier
notification.smtp.server=host-of-smtp
notification.smtp.port=25
notification.smtp.tls=true
notification.smtp.username=some-user
notification.smtp.password=some-password
notification.smtp.truststore.pkcs12.file=truststore.jks
notification.smtp.truststore.pkcs12.password=changeit
notification.recipient=some-user@some-company.com
notification.subject=Kafka Cluster Config Scan result for %TS%
```
The config above sends an email for all offending scan results.

You may also write another notifier to integrate with other systems.
You just need to implment a `io.confluent.ps.tools.notifications.Notifier`.

Note that you may have different profiles for different topics, in such cases, you just need
to implement different scan sets. `set1` may include a set of topics and expected configs, `set2` on the other hand, can be differently configured.

*Note: The set names can be anything simple. e.g. goldset, silverset are fine too!* 

### Out-of-the-box scanner tasks
#### `io.confluent.ps.tools.checks.OfflinePartitionCheck`
This task checks if any partition has `ISR` less than topic configured `min.insync.replicas`.
This task require no parameters.

#### `io.confluent.ps.tools.checks.TopicConfigChecks`
This task checks if topic configs is what you want it to be.
Example
```
set1.task.1=io.confluent.ps.tools.checks.TopicConfigChecks
set1.task.1.keys-to-check.0.name=confluent.placement.constraints
set1.task.1.keys-to-check.0.value={"version":2,"replicas":[{"count":1,"constraints":{"rack":"DC1"}},{"count":1,"constraints":{"rack":"DC2"}}],"observers":[{"count":1,"constraints":{"rack":"DC1"}},{"count":1,"constraints":{"rack":"DC2"}}],"observerPromotionPolicy":"under-min-isr"}
set1.task.1.keys-to-check.0.isJson=true
set1.task.1.keys-to-check.1.name=retention.ms
set1.task.1.keys-to-check.1.value=604800000
set1.task.1.keys-to-check.2.name=compression.type
set1.task.1.keys-to-check.2.value=producer
set1.task.1.keys-to-check.3.name=retention.bytes
set1.task.1.keys-to-check.3.value=-1
set1.task.1.keys-to-check.4.name=compression.type
set1.task.1.keys-to-check.4.value=producer
set1.task.1.keys-to-check.5.name=something-should-be-null
set1.task.1.keys-to-check.6.value=@@NULL
set1.task.1.keys-to-check.6.name=something-should-be-empty-string
set1.task.1.keys-to-check.6.value=@@EMPTY
```

As you can see above, empty strings can be represented using `@@EMPTY` macro and NULLs can be represented by `@@NULL` macro.

#### `io.confluent.ps.tools.checks.ISRSizeCheck`
This task check if ISR size match expectation.
Example
```
set1.task.2=io.confluent.ps.tools.checks.ISRSizeCheck
set1.task.2.isr.size.should.be=2
```

#### `io.confluent.ps.tools.checks.ReplicaSizeCheck`
This task check if number of replicas matches expectation.
Example
```
set1.task.3=io.confluent.ps.tools.checks.ReplicaSizeCheck
set1.task.3.replica.size.should.be=4
```

#### `io.confluent.ps.tools.checks.LeaderImbalanceCheck`
This task check if leader ship if imbalanced. It also checks for brokers has no leadership as well.
Example:
```
set1.task.4.min.total.partitions.for.alarm=100
set1.task.4.maximum.max-leader-count.to.smallest-leader-count.ratio=1.3
```

In the above example, this check will not complain if the total partition in the cluster is less than 100.
The task will complain if the broker with most leadership of partitions vs broker with least leadership of partitions
ratio is larger or equal to 1.3. (e.g. 30% more)

#### `io.confluent.ps.tools.checks.OnlineBrokerCheck`
This task check if all expected brokers are online. And no other unknown brokers are online.
```
set1.task.5=io.confluent.ps.tools.checks.OnlineBrokerCheck
set1.task.5.online.broker.list.should.be=101,102,201,202
```
This task also by default check if the cluster has an active controller. It will report error if no controller found.



