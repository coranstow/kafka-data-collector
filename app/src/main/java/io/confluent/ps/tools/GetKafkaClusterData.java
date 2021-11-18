package io.confluent.ps.tools;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.apache.kafka.clients.admin.*;
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

            KafkaClusterConfiguration config = new KafkaClusterConfiguration(clusterDescription.clusterId().get());

//            String clusterID = clusterDescription.clusterId().get();
//            Collection<Node> clusterNodes = clusterDescription.nodes().get();
//            Node clusterController = clusterDescription.controller().get();
//            Set<AclOperation> clusterAclOperations = clusterDescription.authorizedOperations().get();

            config.setNodes(clusterDescription.nodes().get());
            config.setController(clusterDescription.controller().get());
            config.setAclOperations(clusterDescription.authorizedOperations().get());

            //Topics
            // TODO -      - describeTopics
            ListTopicsOptions listTopicsOptions = new ListTopicsOptions();
            listTopicsOptions.listInternal(true);
            ListTopicsResult clusterTopicList = client.listTopics(listTopicsOptions);
            DescribeTopicsOptions describeTopicsOptions = new DescribeTopicsOptions();
            describeTopicsOptions.includeAuthorizedOperations(true);
            DescribeTopicsResult clusterTopics = client.describeTopics(clusterTopicList.names().get(), describeTopicsOptions);

            config.setTopicDescriptions(clusterTopics.all().get());



            // TODO - describeACLs
// TODO - describeClientQuotas
// TODO       - describeConfigs
// TODO        - describeConsumerGroups
// TODO        - describeDelegationToken
// TODO        - describeFeatures
// TODO        - describeLogDirs
// TODO        - describeProducers
// TODO:       - describeReplicaLogDirs
// TODO -      - describeTransactions
// TODO -      - describeUserScramCredentials
// TODO -      - listConsumerGroupOffsets
// TODO -      - listOffsets
// TODO -      - listPartitionReassignments
// TODO -      - metrics



            GsonBuilder builder = new GsonBuilder();
            builder.setPrettyPrinting().serializeNulls();
            Gson gson = builder.create();

            System.out.println(gson.toJson(config));



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
