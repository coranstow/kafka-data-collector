package io.confluent.ps.tools.notifications;

import io.confluent.ps.tools.checks.Offense;
import net.wushilin.props.EnvAwareProperties;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class DefaultNotifier implements Notifier {
    private String template = "output-%TS%.txt";
    @Override
    public void configure(EnvAwareProperties properties) {
        String outputFile = properties.getProperty("output.filename.template");
        if(outputFile != null && outputFile.trim().length() > 0) {
            template = outputFile.trim();
        }
    }

    public void notify(List<Offense> offenseList) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MMM-dd'T'HH:mm:ss");
        String dt = sdf.format(new Date());
        String fileName = template.replaceAll("%TS%", dt);
        try(FileOutputStream os = new FileOutputStream(fileName)) {
            if(offenseList == null || offenseList.size() == 0) {
                os.write("Nothing to report".getBytes(StandardCharsets.UTF_8));
                return;
            }
            for(Offense next:offenseList) {
                if(next==null) {
                    continue;
                }
                os.write(next.toString().getBytes(StandardCharsets.UTF_8));
                os.write("\n".getBytes(StandardCharsets.UTF_8));
                os.flush();
            }
        } catch(IOException ex) {
            ex.printStackTrace();
        }
    }
}
