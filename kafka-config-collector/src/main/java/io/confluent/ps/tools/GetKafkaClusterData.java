package io.confluent.ps.tools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.kafka.clients.admin.*;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartitionReplica;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.internals.Topic;
import picocli.CommandLine;

import java.io.*;
import java.util.*;
import java.util.concurrent.Callable;

@CommandLine.Command(name = "GetTopicData", version = "GetTopicData 0.1", mixinStandardHelpOptions = true) 
public class GetKafkaClusterData implements Callable<Integer> {

    protected static final String DEFAULT_BOOTSTRAP_SERVERS = "localhost:9092";
    protected static final String DEFAULT_SCHEMA_REGISTRY =  "http://localhost:8081";
//    protected Logger logger = Logger.getLogger(GetKafkaClusterData.class.getName());

    protected Properties properties = new Properties();

    private AdminClient client;

    @CommandLine.Option(names = { "-c", "--config-file" }, description = "A file containing the properties necessary to connect to a Kafka cluster") 
    String configFileName = null;

    @CommandLine.Option(names = { "-b", "--bootstrap-servers" }, description = "The Bootstrap Server(s) string") 
    String bootstrapServers = null;

    @CommandLine.Option(names = {"-o", "--output"}, description = "The output JSON file")
    String outputFile = "get-kafka-cluster-data-output.json";

    @CommandLine.Option(names = {"--schema-registry"})
    protected String schemaRegistryURL;
    
    @CommandLine.Option(names = {"--enable-monitoring-interceptor"},
    description = "Enable MonitoringInterceptors (for Control Center)")


    public void readConfigFile(Properties properties) {
        if (configFileName != null) {
//            logger.info("Reading config file " + configFileName);

            try (InputStream inputStream = new FileInputStream(configFileName)) {
                Reader reader = new InputStreamReader(inputStream);

                properties.load(reader);
//                logger.info(properties.entrySet()
//                            .stream()
//                            .map(e -> e.getKey() + " : " + e.getValue())
//                            .collect(Collectors.joining(", ")));
            } catch (FileNotFoundException e) {
                e.printStackTrace();
//                logger.severe("Inputfile " + configFileName + " not found");
                System.exit(1);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else {
//            logger.warning("No config file specified");
        }
    }

    protected void createProperties() {
        // properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, DEFAULT_BOOTSTRAP_SERVERS);
        // properties.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        // properties.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, DEFAULT_SCHEMA_REGISTRY);

        addProperties(properties);

        readConfigFile(properties);

        if (bootstrapServers != null) {
            properties.put(AdminClientConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        }
        // if (schemaRegistryURL != null) {
        //     properties.put(KafkaAvroSerializerConfig.SCHEMA_REGISTRY_URL_CONFIG, schemaRegistryURL);
        // }
        client = AdminClient.create(properties);


        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down gracefully ...");
            client.close();
        }));
    }

    private void addProperties(Properties properties2) {
    }

    public Integer call() throws Exception {

        createProperties();
        List<String> topics;
        try {

            //Cluster Description
            DescribeClusterResult clusterDescription = client.describeCluster();

            KafkaClusterConfiguration kafkaClusterConfig = new KafkaClusterConfiguration(clusterDescription.clusterId().get());

//            String clusterID = clusterDescription.clusterId().get();
//            Collection<Node> clusterNodes = clusterDescription.nodes().get();
//            Node clusterController = clusterDescription.controller().get();
//            Set<AclOperation> clusterAclOperations = clusterDescription.authorizedOperations().get();

            kafkaClusterConfig.setNodes(clusterDescription.nodes().get());
            kafkaClusterConfig.setController(clusterDescription.controller().get());
            kafkaClusterConfig.setAclOperations(clusterDescription.authorizedOperations().get());

            //Build a list of Broker IDs as a convenience
            List<String> brokerIdStrings = new LinkedList<String>();
            List<Integer> brokerIds = new LinkedList<Integer>();
            for ( Node node : kafkaClusterConfig.getNodes()) {
                brokerIdStrings.add(node.idString());
                brokerIds.add(node.id());
            }



            //Topics
            // TODO -      - describeTopics
            ListTopicsOptions listTopicsOptions = new ListTopicsOptions();
            listTopicsOptions.listInternal(true);
            ListTopicsResult clusterTopicList = client.listTopics(listTopicsOptions);
            DescribeTopicsOptions describeTopicsOptions = new DescribeTopicsOptions();
            describeTopicsOptions.includeAuthorizedOperations(true);
            Collection<String> topicNamesList = new LinkedList<String>();
            topicNamesList.addAll(clusterTopicList.names().get());

            DescribeTopicsResult clusterTopics = client.describeTopics(topicNamesList, describeTopicsOptions);

            kafkaClusterConfig.setTopicDescriptions(clusterTopics.all().get());

            // TODO       - describeConfigs
            Collection<ConfigResource> resources = new LinkedList<ConfigResource>();
            for (String brokerId : brokerIdStrings ) {
                resources.add(new ConfigResource(ConfigResource.Type.BROKER, brokerId));
            }
            for (String topicName : topicNamesList) {
                resources.add(new ConfigResource(ConfigResource.Type.TOPIC, topicName));
            }

            DescribeTopicsOptions describeConfigOptions = new DescribeTopicsOptions();
            describeConfigOptions.includeAuthorizedOperations(true);
            DescribeConfigsResult describeConfigsResult = client.describeConfigs(resources); //, describeConfigOptions);
            Map<ConfigResource, Config> configResourceConfigMap = describeConfigsResult.all().get();
            kafkaClusterConfig.setResourceConfigMap(configResourceConfigMap);

            // TODO        - describeLogDirs

            DescribeLogDirsResult describeLogDirsResult = client.describeLogDirs(brokerIds);
            kafkaClusterConfig.setBrokerLogDirsInfoMap(describeLogDirsResult.all().get());

            /* describeReplicaLogDirs
            Not sure it offers us anything over the LogDirs
//
//            Collection<TopicPartitionReplica> replicas = new LinkedList<TopicPartitionReplica>();
//            TopicPartitionReplica topicPartitionReplica = new TopicPartitionReplica("WIKIPEDIABOT", 0, 1);
//            replicas.add(topicPartitionReplica);
//            DescribeReplicaLogDirsResult describeReplicaLogDirsResult = client.describeReplicaLogDirs(replicas);
//            Map<TopicPartitionReplica, DescribeReplicaLogDirsResult.ReplicaLogDirInfo> topicPartitionReplicaReplicaLogDirInfoMap = describeReplicaLogDirsResult.all().get();
//            kafkaClusterConfig.setReplicaLogDirs(topicPartitionReplicaReplicaLogDirInfoMap);
//
*/
            // TODO -      - metrics

            Map<MetricName, ? extends Metric> metrics = client.metrics();
            kafkaClusterConfig.addMetrics(metrics);
            // TODO        - describeFeatures


            // TODO - describeACLs
            // TODO - describeClientQuotas
            // TODO        - describeConsumerGroups
            // TODO        - describeDelegationToken
            // TODO        - describeProducers
            // TODO -      - describeTransactions
            // TODO -      - describeUserScramCredentials
            // TODO -      - listConsumerGroupOffsets
            // TODO -      - listOffsets
            // TODO -      - listPartitionReassignments



            GsonBuilder builder = new GsonBuilder();
            builder.setPrettyPrinting().serializeNulls();
            Gson gson = builder.create();

            try(FileWriter fw = new FileWriter(outputFile)) {
                fw.write(gson.toJson(kafkaClusterConfig));
            }



//            System.out.println("ClusterID : " + clusterDescription.clusterId().get());
//            Node controller = clusterDescription.controller().get();
//
//            for (Node node : clusterDescription.nodes().get()) {
//                String output = String.format("Id = %d %s:%d",node.id(), node.host(), node.port());
//                if (node == controller) {
//                    output += " Controller";
//                }
//                System.out.println(output);
//            }





        } catch (InterruptedException  e) {
            e.printStackTrace();
        }

        return null;
    }
    

    public static void main(String[] args) {
        int exitCode = new CommandLine(new GetKafkaClusterData()).execute(args);
        System.exit(exitCode);
    }

   
}
