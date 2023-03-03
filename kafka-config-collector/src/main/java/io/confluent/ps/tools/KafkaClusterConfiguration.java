package io.confluent.ps.tools;

import org.apache.kafka.clients.admin.Config;
import org.apache.kafka.clients.admin.DescribeReplicaLogDirsResult;
import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.Metric;
import org.apache.kafka.common.MetricName;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.TopicPartitionReplica;
import org.apache.kafka.common.acl.AclOperation;
import org.apache.kafka.common.config.ConfigResource;
import org.apache.kafka.common.requests.DescribeLogDirsResponse;

import java.util.*;

public class KafkaClusterConfiguration {


    String id = null;
    Collection<Node> nodes = null;
    Node controller = null;
    Set<AclOperation> aclOperations = null;
    private Map<String, TopicDescription> topicDescriptionMap = null;
    private Map<ConfigResource, Config> resourceConfigMap = new HashMap<>();
    private Map<Integer, Map<String,DescribeLogDirsResponse.LogDirInfo>> brokerLogDirsInfoMap = new HashMap<Integer, Map<String,DescribeLogDirsResponse.LogDirInfo>>();
    private Map<TopicPartitionReplica, DescribeReplicaLogDirsResult.ReplicaLogDirInfo> replicaLogDirs = new HashMap<TopicPartitionReplica, DescribeReplicaLogDirsResult.ReplicaLogDirInfo>();
    private HashMap<MetricName, Metric> metrics = new HashMap<MetricName, Metric>();

    public Map<String, TopicDescription> getTopicDescriptionMap() {
        return topicDescriptionMap;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Collection<Node> getNodes() {
        return nodes;
    }

    public void setNodes(Collection<Node> nodes) {
        this.nodes = nodes;
    }

    public Node getController() {
        return controller;
    }

    public void setController(Node controller) {
        this.controller = controller;
    }

    public Set<AclOperation> getAclOperations() {
        return aclOperations;
    }

    public void setAclOperations(Set<AclOperation> aclOperations) {
        this.aclOperations = aclOperations;
    }


    public KafkaClusterConfiguration(String id) {
        this.id = id;
    }


    public void setTopicDescriptions(Map<String, TopicDescription> stringTopicDescriptionMap) {
        this.topicDescriptionMap = stringTopicDescriptionMap;
    }

    public void addBrokerConfig(Map<ConfigResource, Config> configResourceConfigMap) {

    }

    public void setResourceConfigMap(Map<ConfigResource, Config> configResourceConfigMap) {
        resourceConfigMap.putAll(configResourceConfigMap);
    }

    public void setBrokerLogDirsInfoMap(Map<Integer, Map<String, DescribeLogDirsResponse.LogDirInfo>> integerMapMap) {
        brokerLogDirsInfoMap.putAll(integerMapMap);
    }

    public void setReplicaLogDirs(Map<TopicPartitionReplica, DescribeReplicaLogDirsResult.ReplicaLogDirInfo> replicaLogDirs) {
        this.replicaLogDirs = replicaLogDirs;
    }

    public Map<TopicPartitionReplica, DescribeReplicaLogDirsResult.ReplicaLogDirInfo> getReplicaLogDirs() {
        return replicaLogDirs;
    }

    public void addMetrics(Map<MetricName, ? extends Metric> metrics) {
        this.metrics.putAll(metrics);
    }
}