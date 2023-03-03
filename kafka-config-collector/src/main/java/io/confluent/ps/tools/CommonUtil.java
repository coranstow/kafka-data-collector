package io.confluent.ps.tools;

import com.jayway.jsonpath.DocumentContext;
import net.minidev.json.JSONArray;
import net.wushilin.props.EnvAwareProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class CommonUtil {
    public static Map<Integer, String> getNumberedConfigs(EnvAwareProperties props, String prefix, String suffix) {
        Map<Integer, String> result = new TreeMap<Integer, String>();
        for(int i = 0; i < 3000; i++) {
            String key = prefix + "." + i + "." + suffix;
            if(suffix == null) {
                key = prefix + "." + i;
            }
            String value = props.getProperty(key);
            if(value == null) {
                continue;
            }
            result.put(i, value.trim());
        }
        return result;
    }
    public static Map<Integer, String> getNumberedConfigs(EnvAwareProperties props, String prefix) {
        return getNumberedConfigs(props, prefix, null);
    }
    public static List<String> getTopicNames(DocumentContext root) {
        JSONArray array = (JSONArray)root.read("$.topicDescriptionMap.*.name");
        List<String> topicNames = new ArrayList<>();
        for(int i = 0; i < array.size(); i++) {
            String next = (String)array.get(i);
            topicNames.add(next);
        }
        return topicNames;
    }
}
