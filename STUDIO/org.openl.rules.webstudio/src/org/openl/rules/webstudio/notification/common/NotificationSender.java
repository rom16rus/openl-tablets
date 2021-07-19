package org.openl.rules.webstudio.notification.common;

import org.openl.rules.security.standalone.persistence.OpenLUser;

public interface NotificationSender {

    void send(OpenLUser sender, OpenLUser recipient, String subject, String content);
}
