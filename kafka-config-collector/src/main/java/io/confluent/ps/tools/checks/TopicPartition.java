package io.confluent.ps.tools.checks;

public class TopicPartition {
    public String clusterId;
    public String topicName;
    public int partition;

    public TopicPartition(String clusterId, String topicName, int partition) {
        this.clusterId = clusterId;
        this.topicName = topicName;
        this.partition = partition;
    }

    public String toString() {
        if(topicName == null || topicName.trim().length() == 0) {
            return String.format("clusterId=%s", clusterId);
        } else {
            if (partition >= 0) {
                return String.format("clusterId=%s/topic=%s/partition=%d", clusterId, topicName, partition);
            } else {
                return String.format("clusterId=%s/topic=%s", clusterId, topicName);
            }
        }
    }
}
