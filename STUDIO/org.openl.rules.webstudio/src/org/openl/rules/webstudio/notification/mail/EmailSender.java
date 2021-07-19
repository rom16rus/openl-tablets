package org.openl.rules.webstudio.notification.mail;

import org.openl.rules.security.standalone.persistence.OpenLUser;
import org.openl.rules.webstudio.notification.common.NotificationSender;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;

public class EmailSender implements NotificationSender {

    private final Logger logger = LoggerFactory.getLogger(EmailSender.class);

    private String from;
    private String personal;

    private JavaMailSender javaMailSender;

    public void setJavaMailSender(JavaMailSender javaMailSender) {
        this.javaMailSender = javaMailSender;
    }

    @Override
    public void send(OpenLUser sender, OpenLUser recipient, String subject, String content) {
        // TODO
        // String senderMail = sender!=null? sender.getEmail() : "system.email.value";
        String senderMail = "abcd@gmail.com";
        System.out.println("message was sent");
        // MimeMessage message = javaMailSender.createMimeMessage();
        // MimeMessageHelper helper;
        // try {
        // helper = new MimeMessageHelper(message, true);
        // helper.setTo(senderMail);
        // helper.setFrom(from, personal);
        // helper.setText(content);
        // helper.setSubject(subject);
        //
        // javaMailSender.send(message);
        // } catch (Exception e) {
        // logger.error(e.getMessage(), e);
        // }
    }
}
