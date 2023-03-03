package io.confluent.ps.tools;

import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import net.minidev.json.JSONArray;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class KafkaConfigInspection {
    public static final String SINGLE_PARTITION_TOPICS = "_schemas,_confluent_command,connect-cluster-configs,_confluent_balancer_api_state,_confluent_secrets,_confluent-ksql-default__command_topic";
    public static List<String> additionalSinglePartitionTopics = new ArrayList<>();
    public static List<String> additionalCompactTopics = new ArrayList<>();
    public static void main(String[] args) throws IOException {
        //args = new String[]{"output.json", "1-partition-topics.txt", "compact-topics.txt"};
        if (args.length < 1) {
            System.err.println("Usage: KafkaConfigInspection <kafka_config_json_file> [additional-topics-file-that-should-be-one-partition] [additional-topics-file-that-should-be-compact]");
            System.exit(1);
        }

        if(args.length > 1) {
            additionalSinglePartitionTopics = readFileLines(args[1]);
        }
        if(args.length > 2) {
            additionalCompactTopics = readFileLines(args[2]);
        }
        String content = readFile(args[0]);
        DocumentContext pathRoot = JsonPath.parse(content);
        outputByReplicationFactor(pathRoot);
        outputByPartitionCount(pathRoot);
        outputPartitionNotOne(pathRoot);
        outputPartitionOnlyOne(pathRoot);
        outputCleanupPolicy(pathRoot);
        outputMinISRGreaterOrEqualToRF(pathRoot);
    }

    private static void outputMinISRGreaterOrEqualToRF(DocumentContext pathRoot) {
        JSONArray topicNames = pathRoot.read("$.topicDescriptionMap.*.name");
        System.out.println("[Section min.insync.replicas >= replication.factor]");
        for(int i = 0; i < topicNames.size(); i++) {
            String nextTopic = (String)topicNames.get(i);
            String path = "$.resourceConfigMap[\"ConfigResource(type\u003dTOPIC\\, name\u003d\u0027" + nextTopic + "\u0027)\"].entries['min.insync.replicas'].value";
            int minISR = 0;
            try {
                minISR = Integer.parseInt(pathRoot.read(path));
            } catch(Exception ex) {
                System.out.println("      " + nextTopic + ": No MIR defined");
                continue;
            }

            String replicaCountPath = "$.topicDescriptionMap['"+nextTopic+"'].partitions[0].replicas";
            JSONArray replicas = pathRoot.read(replicaCountPath);
            int replicaCount = replicas.size();
            if(replicaCount <= minISR) {
                System.out.println("      " + nextTopic + " MIR: " + minISR + " RF: " + replicaCount);
            }
        }
    }
    private static void outputPartitionOnlyOne(DocumentContext pathRoot) {
        JSONArray topicNames = pathRoot.read("$.topicDescriptionMap.*.name");
        Set<String> theCompactTopics = getCompactTopicsDefault(pathRoot);
        Set<String> toCheck = new HashSet<>();
        toCheck.addAll(split(SINGLE_PARTITION_TOPICS));
        toCheck.addAll(additionalSinglePartitionTopics);

        System.out.println("[Section Partitions == 1]");
        for(int i = 0; i < topicNames.size(); i++) {
            String topicName = (String)topicNames.get(i);
            if(toCheck.contains(topicNames.get(i)) || theCompactTopics.contains(topicName) || topicName.startsWith("_")) {
                // this is okay to have count of 1
                continue;
            }
            String path = "$.topicDescriptionMap['"+ topicName + "'].partitions";
            JSONArray partitions = pathRoot.read(path);
            if (partitions.size() == 1) {
                System.out.println("      " + topicName);
            }
        }

    }

    private static Set<String> getCompactTopicsDefault(DocumentContext pathRoot) {
        JSONArray topicNames = pathRoot.read("$.topicDescriptionMap.*.name");
        Set<String> allTopics = new HashSet<>();
        for(int i = 0; i < topicNames.size(); i++) {
            allTopics.add((String)topicNames.get(i));
        }
        Set<String> toCheck = new HashSet<>();
        for(String next:allTopics) {
            // add _confluent-controlcenter- topics
            if(next.startsWith("_confluent-controlcenter") && (next.endsWith("-changelog") || next.endsWith("-repartition"))) {
                if(!next.endsWith("-MetricsAggregateStore-repartition")) {
                    toCheck.add(next);
                }
            }
            if(next.equals("connect-cluster-status") || next.equals("connect-status")) {
                toCheck.add(next);
            }
            if(next.equals("connect-cluster-configs") || next.equals("connect-configs")) {
                toCheck.add(next);
            }
            if(next.equals("connect-cluster-offsets") || next.equals("connect-offsets")) {
                toCheck.add(next);
            }
            if(next.equals("_confluent-metadata-auth")) {
                toCheck.add(next);
            }
            if(next.equals("_confluent-command")) {
                toCheck.add(next);
            }
            if(next.equals("_confluent_balancer_api_state")) {
                toCheck.add(next);
            }
            if(next.equals("_schemas")) {
                toCheck.add(next);
            }
            if(next.equals("_confluent-secrets")) {
                toCheck.add(next);
            }
            if(next.equals("__consumer_offsets")) {
                toCheck.add(next);
            }
        }
        toCheck.addAll(additionalCompactTopics);
        return toCheck;
    }

    private static void outputCleanupPolicy(DocumentContext pathRoot) {
        Set<String> toCheck = getCompactTopicsDefault(pathRoot);
        for(String nextTopic:toCheck) {
            try {
                //Object policy =  pathRoot.read("$.resourceConfigMap");
                String path = "$.resourceConfigMap[\"ConfigResource(type\u003dTOPIC\\, name\u003d\u0027" + nextTopic + "\u0027)\"].entries['cleanup.policy'].value";
                // String path = "$.resourceConfigMap[\"ConfigResource(type\u003dTOPIC, name\u003d\u0027" + nextTopic + "\u0027)\"]";
                String policy = (String) pathRoot.read(path);
                if (policy.indexOf("compact") == -1) {
                    System.out.println("      " + nextTopic);
                }
            } catch(Exception ex) {
                continue;
            }
        }
    }

    private static List<String> split(String input) {
        List<String> result = new ArrayList<>();
        if(input == null || input.trim().length() == 0) {
            return result;
        }
        String[] tokens = input.split(",");
        for(String next:tokens) {
            if(next != null && next.trim().length() > 0) {
                result.add(next.trim());
            }
        }
        return result;
    }
    private static void outputPartitionNotOne(DocumentContext pathRoot) {
        Set<String> toCheck = new HashSet<>();
        toCheck.addAll(split(SINGLE_PARTITION_TOPICS));
        toCheck.addAll(additionalSinglePartitionTopics);

        System.out.println("[Section Partitions != 1]");
        for(String nextSinglePartitionTopic:toCheck) {
            String path = "$.topicDescriptionMap['"+ nextSinglePartitionTopic + "'].partitions";
            try {
                JSONArray partitions = pathRoot.read(path);
                if (partitions.size() != 1) {
                    System.out.println("      " + nextSinglePartitionTopic);
                }
            } catch(PathNotFoundException ex) {
                continue;
            }
        }
    }

    private static <K,V> void add(TreeMap<K, List<V>> map, K key, V value) {
        List<V> target = map.get(key);
        if(target  == null) {
            target = new ArrayList<V>();
            map.put(key, target);
        }
        target.add(value);
    }
    private static void outputByPartitionCount(DocumentContext pathRoot) {
        JSONArray topicNames = pathRoot.read("$.topicDescriptionMap.*.name");

        System.out.println("[Section Group By Partition Count]");
        TreeMap<Integer, List<String>> aggregated = new TreeMap<>();
        for(int i = 0; i < topicNames.size(); i++) {
            String nextTopic = (String)topicNames.get(i);
            String partitionCountPath = "$.topicDescriptionMap['"+nextTopic+"'].partitions";
            JSONArray partitions = pathRoot.read(partitionCountPath);
            add(aggregated, partitions.size(), nextTopic);
        }
        for(Map.Entry<Integer, List<String>> next:aggregated.entrySet()) {
            int partitionCount = next.getKey();
            List<String> topics = next.getValue();
            System.out.println(">> Topics has " + partitionCount + " partitions:");
            for(String nextTopic:topics) {
                System.out.println("      " + nextTopic);
            }
        }
    }

    private static void outputByReplicationFactor(DocumentContext pathRoot) {
        JSONArray topicNames = pathRoot.read("$.topicDescriptionMap.*.name");

        TreeMap<Integer, List<String>> aggregated = new TreeMap<>();
        System.out.println("[Section Group By Replica Count]");

        for(int i = 0; i < topicNames.size(); i++) {
            String nextTopic = (String)topicNames.get(i);
            String replicaCountPath = "$.topicDescriptionMap['"+nextTopic+"'].partitions[0].replicas";
            JSONArray replicas = pathRoot.read(replicaCountPath);
            add(aggregated, replicas.size(), nextTopic);
        }

        for(Map.Entry<Integer, List<String>> next:aggregated.entrySet()) {
            int replicaCount = next.getKey();
            List<String> topics = next.getValue();
            System.out.println(">> Topics has " + replicaCount + " replicas:");
            for(String nextTopic:topics) {
                System.out.println("      " + nextTopic);
            }
        }
    }

    private static List<String> readFileLines(String input) throws IOException {
        List<String> result = new ArrayList<>();
        try(FileReader fr = new FileReader(input); BufferedReader br = new BufferedReader(fr)) {
            String buffer;
            while((buffer = br.readLine()) != null) {
                result.add(buffer);
            }
        }
        return result;
    }
    private static String readFile(String input) throws IOException {
        StringBuilder resultStringBuilder = new StringBuilder();
        try (FileReader fr = new FileReader(input); BufferedReader br = new BufferedReader(fr)) {
            String line;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.append(line).append("\n");
            }
        }
        return resultStringBuilder.toString();
    }
}
