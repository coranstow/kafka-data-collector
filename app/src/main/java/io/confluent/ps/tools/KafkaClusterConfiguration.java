package io.confluent.ps.tools;

import org.apache.kafka.clients.admin.TopicDescription;
import org.apache.kafka.common.Node;
import org.apache.kafka.common.acl.AclOperation;

import java.util.Collection;
import java.util.Map;
import java.util.Set;

public class KafkaClusterConfiguration {


    String id = null;
    Collection<Node> nodes = null;
    Node controller = null;
    Set<AclOperation> aclOperations = null;
    private Map<String, TopicDescription> topicDescriptionMap = null;


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
}
