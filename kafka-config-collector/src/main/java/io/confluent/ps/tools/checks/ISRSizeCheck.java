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

public class ISRSizeCheck implements ConfigCheckTask {
    private EnvAwareProperties config;
    @Override
    public List<Offense> getOffendingTopicPartition(DocumentContext root, TopicFilter tf) {
        String key = "isr.size.should.be";
        String childName = "isr";
        String shouldBe = config.getProperty(key);
        if(shouldBe == null || shouldBe.trim().length() == 0) {
            System.err.println(key + " must be defined for " +this.getClass().getSimpleName()+ " task!");
            return null;
        }
        shouldBe = shouldBe.trim();
        int shouldBeInt = 0;
        try {
            shouldBeInt = Integer.parseInt(shouldBe);
        } catch(Exception ex) {
            System.err.println(key + " must be integer (" + shouldBe + ")");
            return null;
        }
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
                JSONArray target = (JSONArray) thePartition.get(childName);
                Integer partition = (Integer) thePartition.get("partition");
                int partitionInt = partition.intValue();
                int actualSize =  target.size();
                if(actualSize != shouldBeInt) {
                    // Found mismatch
                    TopicPartition tpf = partitionFor(root, next, partitionInt);
                    Offense newOffense = new Offense(this.getClass().getSimpleName(), tpf);
                    String description = String.format("Expected size is %d actual is %d", shouldBeInt, actualSize);
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
}
