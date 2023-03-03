package io.confluent.ps.tools.notifications;

import io.confluent.ps.tools.checks.Offense;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeBodyPart;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.internet.MimeMultipart;
import net.wushilin.props.EnvAwareProperties;

import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class SMTPNotifier implements Notifier {
    private EnvAwareProperties config = null;
    @Override
    public void configure(EnvAwareProperties properties) {
        this.config = properties;
    }

    public void notify(List<Offense> offenseList) {
        try {
            notify1(offenseList);
        } catch(Exception ex) {
            ex.printStackTrace();
            System.out.println("Failed to send email: " + ex.getMessage());
        }
    }
    private void notify1(List<Offense> offenseList) throws Exception {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MMM-dd'T'HH:mm:ss");
        String dt = sdf.format(new Date());

        Session session = Session.getInstance(config, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(config.getProperty("mail.smtp.username"), config.getProperty("mail.smtp.password"));
            }
        });
        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(config.getProperty("mail.from")));
            message.setRecipients(
                    Message.RecipientType.TO, InternetAddress.parse(config.getProperty("mail.to")));
            String subject = config.getProperty("mail.subject");
            if (subject != null) {
                subject = subject.replaceAll("%TS%", dt);
                message.setSubject(subject);
            }
        /*
        Properties prop = new Properties();
        prop.put("mail.smtp.auth", true);
        prop.put("mail.smtp.starttls.enable", "true");
        prop.put("mail.smtp.host", "smtp.mailtrap.io");
        prop.put("mail.smtp.port", "25");
        prop.put("mail.smtp.ssl.trust", "smtp.mailtrap.io");
         */
            ByteArrayOutputStream os = new ByteArrayOutputStream();
            try {
                if (offenseList == null || offenseList.size() == 0) {
                    return;
                }
                os.write("<div style='font-family: Tahoma; font-size:18; padding: 5px; margin:5px; font-size:14'>\n".getBytes(StandardCharsets.UTF_8));
                os.write("Kafka Config Scanner found the following potential issues:\n".getBytes(StandardCharsets.UTF_8));
                os.write("<OL>\n".getBytes(StandardCharsets.UTF_8));
                for (Offense next : offenseList) {
                    if (next == null) {
                        continue;
                    }
                    os.write(("<li style='font-color:orange'>" + next.toString() + "</li>").getBytes(StandardCharsets.UTF_8));
                    os.write("\n".getBytes(StandardCharsets.UTF_8));
                    os.flush();
                }
                os.write("</OL>\n".getBytes(StandardCharsets.UTF_8));
                os.write("</div>\n".getBytes(StandardCharsets.UTF_8));
            } catch (IOException ex) {
                ex.printStackTrace();
            } finally {
                os.flush();
                os.close();
            }
            String msg = new String(os.toByteArray(), StandardCharsets.UTF_8);

            MimeBodyPart mimeBodyPart = new MimeBodyPart();
            mimeBodyPart.setContent(msg, "text/html; charset=utf-8");

            Multipart multipart = new MimeMultipart();
            multipart.addBodyPart(mimeBodyPart);

            message.setContent(multipart);
            Transport.send(message);
        } finally {
        }
    }
}
