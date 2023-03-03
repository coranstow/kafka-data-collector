package io.confluent.ps.tools;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import io.confluent.ps.tools.checks.ConfigCheckTask;
import io.confluent.ps.tools.checks.Offense;
import io.confluent.ps.tools.checks.TopicFilter;
import io.confluent.ps.tools.checks.TopicPartition;
import io.confluent.ps.tools.notifications.DefaultNotifier;
import io.confluent.ps.tools.notifications.Notifier;
import net.wushilin.props.EnvAwareProperties;
import picocli.CommandLine;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class TopicConfigScanner {
    String configFileName = null;
    String jsonFile = null;
    public TopicConfigScanner(String jsonFile, String configFileName) {
        this.jsonFile = jsonFile;
        this.configFileName = configFileName;
    }

    public Integer call() throws IOException {
        System.out.println(jsonFile);
        String content = readFile(jsonFile);
        DocumentContext pathRoot = JsonPath.parse(content);

        EnvAwareProperties config = EnvAwareProperties.fromPath(configFileName);
        String scanList = config.getProperty("scan.list", "");
        if(scanList == null || scanList.trim().length() == 0) {
            System.out.println("No scan.list defined in " + configFileName);
        }
        scanList = scanList.trim();
        String[] sets = scanList.split(",|;");
        List<Offense> result = new ArrayList<>();
        for(String next:sets) {
            next = next.trim();
            if(next.length() == 0) {
                continue;
            }
            EnvAwareProperties subset = config.partition(next);
            List<Offense> newOffenses = subscan(pathRoot, subset, next);
            if(newOffenses == null) {
                continue;
            }
            result.addAll(newOffenses);
        }

        EnvAwareProperties notificiationConfig = config.partition("notification");
        String className = notificiationConfig.getProperty("class", DefaultNotifier.class.getName());
        try {
            System.out.printf("Reporting offending topics using %s\n", className);
            Notifier notif = (Notifier) Class.forName(className).getDeclaredConstructor().newInstance();
            notif.configure(notificiationConfig);
            notif.notify(result);
            System.out.printf("Reported %d cases\n", result.size());
        } catch (Exception e) {
            System.err.println("Unable to report the offending topic configs.");
            e.printStackTrace();
        }
        return 0;
    }

    private List<Offense> subscan(DocumentContext root, EnvAwareProperties subset, String set) {
        TopicFilter tf = new TopicFilter();
        tf.configure(subset);
        System.out.println("Performing scan for set [" + set + "]");
        if("false".equalsIgnoreCase(subset.getProperty("enabled"))) {
            System.out.println("Set [" + set + "] is disabled. Skipping");
            return null;
        }
        List<Offense> result = new ArrayList<>();
        Map<Integer, String> setTasks = CommonUtil.getNumberedConfigs(subset, String.format("task"), null);
        for(Map.Entry<Integer, String> entry:setTasks.entrySet()) {
            int number = entry.getKey();
            String className = entry.getValue();
            System.out.printf("Processing with scanner %d (%s)\n", number, className);
            try {
                ConfigCheckTask task = (ConfigCheckTask)Class.forName(className).getDeclaredConstructor().newInstance();
                task.configure(subset.partition(String.format("task.%d", number)));
                List<Offense> offending = task.getOffendingTopicPartition(root, tf);
                if(offending == null || offending.size() == 0) {
                    System.out.printf("Task %d (%s) reported no offending topic/partitions\n", number, className);
                } else {
                    System.out.printf("Task %d (%s) reported %d offending topic/partitions\n", number, className,
                            offending.size());
                   result.addAll(offending);
                }
            } catch(Exception ex) {
                //ex.printStackTrace();
                System.err.println("Unable to instantiate class " + className + ":" + ex.getMessage());
            }
        }
        return result;
    }


    public static void main(String[] args) throws IOException {
        if(args.length != 2) {
            System.out.println("Usage: TopicConfigScanner <cluster-data-from-GetKafkaClusterData> <scan-config.properties>");
            System.exit(1);
        }
        int exitCode = new TopicConfigScanner(args[0], args[1]).call();
        System.exit(exitCode);
    }

    private static List<String> readFileLines(String input) throws IOException {
        List<String> result = new ArrayList<>();
        try(FileReader fr = new FileReader(input); BufferedReader br = new BufferedReader(fr)) {
            String buffer;
            while((buffer = br.readLine()) != null) {
                result.add(buffer);
            }
        }
        return result;
    }

    private static String readFile(String input) throws IOException {
        StringBuilder resultStringBuilder = new StringBuilder();
        try (FileReader fr = new FileReader(input); BufferedReader br = new BufferedReader(fr)) {
            String line;
            while ((line = br.readLine()) != null) {
                resultStringBuilder.append(line).append("\n");
            }
        }
        return resultStringBuilder.toString();
    }
}
