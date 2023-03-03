package io.confluent.ps.tools.checks;

import com.jayway.jsonpath.DocumentContext;
import io.confluent.ps.tools.CommonUtil;
import net.minidev.json.JSONArray;
import net.wushilin.props.EnvAwareProperties;

import java.util.*;

public class OnlineBrokerCheck implements ConfigCheckTask {
    private EnvAwareProperties config;
    @Override
    public List<Offense> getOffendingTopicPartition(DocumentContext root, TopicFilter tf) {
        String key = "online.broker.list.should.be";
        String onlineBrokerString = config.getProperty(key);
        if(onlineBrokerString == null || onlineBrokerString.trim().length() == 0) {
            System.err.println(key + " must be defined for " +this.getClass().getSimpleName()+ " task!");
            return null;
        }

        Set<String> brokerIds = new HashSet<>();
        String[] tokens = onlineBrokerString.split(",");
        for(String next:tokens) {
            if(next != null) {
                brokerIds.add(next.trim());
            }
        }
        List<Offense> result = new ArrayList<>();
        JSONArray onlineIds = root.read("$.nodes[*].id");
        Set<String> actuallyOnlineIds = new HashSet<>();
        for(int i = 0; i < onlineIds.size(); i++) {
            String nextId = String.valueOf(onlineIds.get(i));
            actuallyOnlineIds.add(nextId);
            if(!brokerIds.contains(nextId)) {
                TopicPartition tp = partitionFor(root, null, -1);
                Offense no = new Offense(this.getClass().getSimpleName(), tp);
                String description = String.format("Unexpected online broker id %s not in %s", nextId, onlineBrokerString);
                no.setDescription(description);
                result.add(no);
            }
        }
        for(String next:brokerIds) {
            if(!actuallyOnlineIds.contains(next)) {
                TopicPartition tp = partitionFor(root, null, -1);
                Offense no = new Offense(this.getClass().getSimpleName(), tp);
                String description = String.format("Expected online broker id %s not online", next);
                no.setDescription(description);
                result.add(no);
            }
        }

        boolean hasController = false;
        try {
            int controllerId = root.read("$.controller.id");
            hasController = true;
        } catch(Exception x) {
        }
        if(!hasController) {
            TopicPartition tp = partitionFor(root, null, -1);
            Offense no = new Offense(this.getClass().getSimpleName(), tp);
            String description = String.format("Cluster has no controller");
            no.setDescription(description);
            result.add(no);
        }
        return result;
    }

    @Override
    public void setConfiguration(EnvAwareProperties config) {
        this.config = config;
    }
}
