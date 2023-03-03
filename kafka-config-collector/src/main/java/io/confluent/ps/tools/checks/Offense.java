package io.confluent.ps.tools.checks;

public class Offense {
    private String ruleName;
    private TopicPartition topicPartition;

    private String description;

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Offense(String ruleName, TopicPartition topicPartition) {
        this.ruleName = ruleName;
        this.topicPartition = topicPartition;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public TopicPartition getTopicPartition() {
        return topicPartition;
    }

    public void setTopicPartition(TopicPartition topicPartition) {
        this.topicPartition = topicPartition;
    }

    public String toString() {
        if(description == null) {
            return String.format("%s: %s", this.ruleName, this.topicPartition);
        }
        return String.format("%s: %s (%s)", this.ruleName, this.topicPartition, description);
    }
}
