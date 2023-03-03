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

