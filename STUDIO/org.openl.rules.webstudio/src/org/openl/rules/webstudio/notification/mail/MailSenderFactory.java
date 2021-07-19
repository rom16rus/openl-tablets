package org.openl.rules.webstudio.notification.mail;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.mail.javamail.JavaMailSender;

public class MailSenderFactory implements FactoryBean<JavaMailSender>, InitializingBean {

    private String host;
    private Integer port;
    private String userName;
    private String password;

    private JavaMailSender javaMailSender;

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    @Override
    public Class<?> getObjectType() {
        return JavaMailSender.class;
    }

    @Override
    public void afterPropertiesSet() {
        MailConfiguration mailConfiguration = new MailConfiguration();
        mailConfiguration.setUsername(userName);
        mailConfiguration.setPassword(password);
        mailConfiguration.setSmtpHost(host);
        mailConfiguration.setSmtpPort(port);

        javaMailSender = MailConfigurationUtils.createMailSender(mailConfiguration);
    }

    @Override
    public JavaMailSender getObject() {
        return javaMailSender;
    }

    @Override
    public boolean isSingleton() {
        return false;
    }
}
