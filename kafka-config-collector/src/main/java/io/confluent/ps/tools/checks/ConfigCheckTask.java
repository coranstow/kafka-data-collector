package io.confluent.ps.tools.checks;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.TypeRef;
import net.wushilin.props.EnvAwareProperties;

import java.util.List;

public interface ConfigCheckTask {
    public List<Offense> getOffendingTopicPartition(DocumentContext root, TopicFilter tf);
    default TopicPartition partitionFor(DocumentContext root, String topicName, int id) {
        String clusterId = root.read("$.id");
        return new TopicPartition(clusterId, topicName, id);
    }

    default void configure(EnvAwareProperties config) {
        System.out.println("Configuring " + this.getClass().getName() + " with " + config);
        setConfiguration(config);
    }

    public void setConfiguration(EnvAwareProperties config);
}
