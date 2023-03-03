package io.confluent.ps.tools.checks;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.DocumentContext;
import io.confluent.ps.tools.CommonUtil;
import net.minidev.json.JSONObject;
import net.wushilin.props.EnvAwareProperties;

import java.util.*;

public class TopicConfigChecks implements ConfigCheckTask {
    private EnvAwareProperties config;
    private static final String MACRO_PREFIX = "@@";
    private static Map<String, String> MACROS = new HashMap<>();
    static {
        MACROS.put(MACRO_PREFIX + "NULL", null);
        MACROS.put(MACRO_PREFIX + "EMPTY", "");
    }
    @Override
    public List<Offense> getOffendingTopicPartition(DocumentContext root, TopicFilter tf) {
        LinkedHashMap configMap = root.read("$.resourceConfigMap");
        List<String> topicNames = CommonUtil.getTopicNames(root);
        List<Offense> result = new ArrayList<>();
        String configPrefix = "keys-to-check";
        Map<Integer, String> keysToCheck = CommonUtil.getNumberedConfigs(config, configPrefix, "name");
        Map<Integer, String> valuesToCheck = CommonUtil.getNumberedConfigs(config, configPrefix, "value");
        Map<Integer, String> isJsonToCheck = CommonUtil.getNumberedConfigs(config, configPrefix, "isJson");

        for(String nextTopic:topicNames) {
            if(!tf.accept(nextTopic)) {
                System.out.println("Ignored topic " + nextTopic);
                continue;
            } else {
                System.out.println("Processing topic " + nextTopic);
            }
            for(Map.Entry<Integer, String> nextKey: keysToCheck.entrySet()) {
                Integer id = nextKey.getKey();
                String configName = nextKey.getValue();
                String expectedValue = valuesToCheck.get(id);
                if(expectedValue == null) {
                    System.err.println("No value defined for key " + configName + " index " + id);
                    continue;
                }
                expectedValue = expectedValue.trim();
                if(expectedValue.startsWith(MACRO_PREFIX)) {
                    if(MACROS.containsKey(expectedValue)) {
                        expectedValue = MACROS.get(expectedValue);
                    }
                }
                boolean isJson = false;
                if(isJsonToCheck.containsKey(id)) {
                    if("true".equalsIgnoreCase(isJsonToCheck.get(id).trim())) {
                        isJson = true;
                    }
                }

                String actualValue = getConfigForTopic(configMap, nextTopic, configName);
                boolean equals = false;
                if(expectedValue == null) {
                    if(actualValue == null) {
                        equals = true;
                    } else {
                        equals = false;
                    }
                } else if(isJson) {
                    if(actualValue == null) {
                        equals = false;
                    } else {
                        equals = jsonEqual(expectedValue, actualValue);
                    }
                } else {
                    equals = expectedValue.equals(actualValue);
                }
                if(!equals) {
                    Offense newOffense = new Offense(TopicConfigChecks.class.getSimpleName(), partitionFor(root, nextTopic, -1));
                    newOffense.setDescription(String.format("%s=[%s] (expect [%s])", configName, actualValue, expectedValue));
                    result.add(newOffense);
                }
            }
        }
        return result;
    }
    private static ObjectMapper mapper = new ObjectMapper();
    private static boolean jsonEqual(String a, String b) {
        try {
            return mapper.readTree(a).equals(mapper.readTree(b));
        } catch(Exception ex) {
            return false;
        }
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
    @Override
    public void setConfiguration(EnvAwareProperties config) {
        this.config = config;
    }
}
