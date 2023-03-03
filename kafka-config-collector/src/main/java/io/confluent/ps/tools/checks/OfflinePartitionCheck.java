package io.confluent.ps.tools.checks;

import com.jayway.jsonpath.DocumentContext;
import io.confluent.ps.tools.CommonUtil;
import io.confluent.ps.tools.checks.ConfigCheckTask;
import io.confluent.ps.tools.checks.Offense;
import io.confluent.ps.tools.checks.TopicFilter;
import io.confluent.ps.tools.checks.TopicPartition;
import net.minidev.json.JSONArray;
import net.wushilin.props.EnvAwareProperties;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class OfflinePartitionCheck implements ConfigCheckTask {
    private EnvAwareProperties config;
    @Override
    public List<Offense> getOffendingTopicPartition(DocumentContext root, TopicFilter tf) {
        LinkedHashMap configMap = root.read("$.resourceConfigMap");

        List<String> topicNames = CommonUtil.getTopicNames(root);
        List<Offense> result = new ArrayList<>();
        for(String next:topicNames) {
            if(!tf.accept(next)) {
                System.out.println("Ignoring topic " + next);
                continue;
            }
            JSONArray partitions = root.read("$.topicDescriptionMap.['" + next + "'].partitions");
            for(int i = 0; i < partitions.size(); i++) {
                LinkedHashMap thePartition = (LinkedHashMap) partitions.get(i);
                JSONArray target = (JSONArray) thePartition.get("isr");
                Integer partition = (Integer) thePartition.get("partition");
                int partitionInt = partition.intValue();
                int actualSize =  target.size();
                String expectedSizeString = getConfigForTopic(configMap, next, "min.insync.replicas");
                int expectedSize = Integer.parseInt(expectedSizeString);
                if(actualSize < expectedSize) {
                    // Found mismatch
                    TopicPartition tpf = partitionFor(root, next, partitionInt);
                    Offense newOffense = new Offense(this.getClass().getSimpleName(), tpf);
                    String description = String.format("MIR is %d actual ISR size %d", expectedSize, actualSize);
                    newOffense.setDescription(description);
                    result.add(newOffense);
                }
            }
        }
        return result;
    }

    @Override
    public void setConfiguration(EnvAwareProperties config) {
        this.config = config;
    }

    private String getConfigForTopic(LinkedHashMap config, String topic, String configKey) {
        String key = String.format("ConfigResource(type\u003dTOPIC, name\u003d\u0027%s\u0027)", topic);
        try {
            return (String)((LinkedHashMap)((LinkedHashMap)((LinkedHashMap)config.get(key)).get("entries")).get(configKey)).get("value");
        } catch(Exception ex) {
            //ex.printStackTrace();
            return null;
        }
    }
}
