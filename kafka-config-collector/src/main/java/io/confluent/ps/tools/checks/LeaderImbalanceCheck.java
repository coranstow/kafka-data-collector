package io.confluent.ps.tools.checks;

import com.jayway.jsonpath.DocumentContext;
import io.confluent.ps.tools.CommonUtil;
import net.minidev.json.JSONArray;
import net.wushilin.props.EnvAwareProperties;

import java.util.*;

public class LeaderImbalanceCheck implements ConfigCheckTask {
    private EnvAwareProperties config;
    @Override
    public List<Offense> getOffendingTopicPartition(DocumentContext root, TopicFilter tf) {
        JSONArray configs = (JSONArray)(root.read("$.topicDescriptionMap[*].partitions"));
        int minNumOfPartitions = 100;
        double maxRatio = 1.3;
        try {
            minNumOfPartitions = Integer.parseInt(config.getProperty("min.total.partitions.for.alarm", "100"));
            maxRatio = Double.parseDouble(config.getProperty("maximum.max-leader-count.to.smallest-leader-count.ratio", "1.3"));
        } catch(Exception ex) {
            System.out.println("Task aborted: " + ex.getMessage());
            System.out.println("Invalid config for min.total.partitions.for.alarm or maximum.max-leader-count.to.smallest-leader-count.ratio");
            return null;
        }
        // count total partitions
        int totalPartitions = 0;
        int hasLeaderCount = 0;
        Map<Object, Integer> leaderCount = new HashMap<Object, Integer>();
        JSONArray nodes = (JSONArray)root.read("$.nodes");
        for(int i = 0 ; i < nodes.size() ;i++ ) {
            leaderCount.put(nodes.get(i), 0);
        }
        List<String> topicNames = CommonUtil.getTopicNames(root);
        // This job ignores topic filter!

        List<Offense> result = new ArrayList<>();
        for(int i = 0; i < configs.size(); i++) {
            JSONArray nextTopicConfig = (JSONArray) configs.get(i);
            for (int j = 0; j < nextTopicConfig.size(); j++) {
                LinkedHashMap nextPartitionConfig = (LinkedHashMap) nextTopicConfig.get(j);
                LinkedHashMap leader = (LinkedHashMap) nextPartitionConfig.get("leader");
                if(leader == null) {
                    System.out.println("Found no leader count");
                } else {
                    hasLeaderCount++;
                    if(leaderCount.containsKey(leader)) {
                        leaderCount.put(leader, leaderCount.get(leader) + 1);
                    } else {
                        leaderCount.put(leader, 1);
                    }
                }
                totalPartitions++;

            }
        }

        if(hasLeaderCount < totalPartitions) {
            TopicPartition tp = partitionFor(root, null, -1);
            Offense newOffense = new Offense(this.getClass().getSimpleName(), tp);
            newOffense.setDescription(String.format("%d partitions has no leader!", totalPartitions - hasLeaderCount));
            result.add(newOffense);
        }

        if(totalPartitions < minNumOfPartitions) {
            System.out.println("Minimum partitions not met. Min = " + minNumOfPartitions + " actual " + totalPartitions);
            return result;
        }

        Map.Entry<Object, Integer> maxNode = findMax(leaderCount);
        Map.Entry<Object, Integer> minNode = findMin(leaderCount);
        System.out.println(String.format("Max load server: %s => %d", maxNode.getKey(), maxNode.getValue()));
        System.out.println(String.format("Min load server: %s => %d", minNode.getKey(), minNode.getValue()));
        if(minNode.getValue() == 0) {
            TopicPartition tp = partitionFor(root, null, -1);
            Offense newOffense = new Offense(this.getClass().getSimpleName(), tp);
            newOffense.setDescription(String.format("Node %s has no leadership at all!", minNode.getKey()));
            result.add(newOffense);
        } else {
            double ratio = ((double)maxNode.getValue())/((double)minNode.getValue());
            System.out.printf("Max to Min ratio is (%d/%d) = %f (%s/%s)\n",
                    maxNode.getValue(), minNode.getValue(), ratio, maxNode.getKey(), minNode.getKey());
            if(ratio >= maxRatio) {
                TopicPartition tp = partitionFor(root, null, -1);
                Offense newOffense = new Offense(this.getClass().getSimpleName(), tp);
                newOffense.setDescription(String.format("Max Load node %s to Min Load node %s ratio is %f", maxNode.getKey(), minNode.getKey(), ratio));
                result.add(newOffense);
            }
        }

        return result;
    }

    private static Map.Entry<Object, Integer> findMax(Map<Object, Integer> elements) {
        if(elements.size() == 0) {
            return null;
        }
        Map.Entry<Object, Integer> result = null;
        for(Map.Entry<Object, Integer> next:elements.entrySet()) {
            int count = next.getValue();
            if(result == null) {
                result = next;
            } else {
                if(count > result.getValue()) {
                    result = next;
                }
            }
        }
        return result;
    }

    private static Map.Entry<Object, Integer> findMin(Map<Object, Integer> elements) {
        if(elements.size() == 0) {
            return null;
        }
        Map.Entry<Object, Integer> result = null;
        for(Map.Entry<Object, Integer> next:elements.entrySet()) {
            int count = next.getValue();
            if(result == null) {
                result = next;
            } else {
                if(count < result.getValue()) {
                    result = next;
                }
            }
        }
        return result;
    }
    @Override
    public void setConfiguration(EnvAwareProperties config) {
        this.config = config;
    }
}
