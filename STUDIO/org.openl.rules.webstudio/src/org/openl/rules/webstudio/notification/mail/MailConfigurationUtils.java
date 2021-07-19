package org.openl.rules.webstudio.notification.mail;

import java.util.Properties;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

public class MailConfigurationUtils {

    public static JavaMailSender createMailSender(MailConfiguration configuration) {
        JavaMailSenderImpl javaMailSender = new JavaMailSenderImpl();
        javaMailSender.setUsername(configuration.getUsername());
        javaMailSender.setPassword(configuration.getPassword());
        javaMailSender.setHost(configuration.getSmtpHost());
        javaMailSender.setPort(configuration.getSmtpPort());

        javaMailSender.setDefaultEncoding("UTF-8");

        Properties properties = javaMailSender.getJavaMailProperties();

        properties.put("mail.smtp.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        properties.put("mail.smtp.socketFactory.port", configuration.getSmtpPort());
        properties.put("mail.smtp.socketFactory.fallback", false);
        properties.put("mail.smtp.auth", true);
        properties.put("mail.smtp.starttls.enable", true);
        properties.put("mail.transport.protocol", "smtp");
        properties.put("mail.mime.charset", "UTF-8");
        properties.put("mail.debug", "false");

        return javaMailSender;

    }
}
