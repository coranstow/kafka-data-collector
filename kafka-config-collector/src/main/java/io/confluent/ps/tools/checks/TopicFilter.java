package io.confluent.ps.tools.checks;

import io.confluent.ps.tools.CommonUtil;
import net.wushilin.props.EnvAwareProperties;

import java.util.*;
import java.util.regex.Pattern;

public class TopicFilter {
    private EnvAwareProperties config;
    private List<Pattern> whiteLists = new ArrayList<>();
    private List<Pattern> blackList = new ArrayList<>();
    private Set<String> include = new HashSet<>();
    private Set<String> exclude = new HashSet<>();
    public void configure(EnvAwareProperties config) {
        this.config = config;
        Collection<String> tmp = CommonUtil.getNumberedConfigs(config, "topics.whitelist.regex").values();
        whiteLists.addAll(toPatterns(tmp));

        tmp = CommonUtil.getNumberedConfigs(config, "topics.blacklist.regex").values();
        blackList.addAll(toPatterns(tmp));

        tmp = CommonUtil.getNumberedConfigs(config, "topics.include").values();
        include.addAll(tmp);
        tmp = CommonUtil.getNumberedConfigs(config, "topics.exclude").values();
        exclude.addAll(tmp);
    }

    private List<Pattern> toPatterns(Collection<String> in) {
        List<Pattern> result = new ArrayList<>();
        if(in != null && in.size() > 0) {
            for (String next : in) {
                try {
                    result.add(Pattern.compile(next));
                } catch (Exception ex) {
                    continue;
                }
            }
        }
        return result;
    }

    private boolean matchAny(String input, List<Pattern> patterns) {
        if(patterns == null || patterns.size() == 0) {
            return false;
        }

        for(Pattern next:patterns) {
            if(next == null) {
                continue;
            }
            if(next.matcher(input).matches()) {
                return true;
            }
        }
        return false;
    }
    public boolean accept(String topicName) {
        if(matchAny(topicName, blackList)) {
            return false;
        }
        if(exclude.contains(topicName)) {
            return false;
        }
        if(include.contains(topicName)) {
            return true;
        }
        if(matchAny(topicName, whiteLists)) {
            return true;
        }
        return false;
    }
}
