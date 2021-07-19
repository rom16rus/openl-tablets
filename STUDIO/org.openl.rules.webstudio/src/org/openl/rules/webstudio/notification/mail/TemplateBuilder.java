package org.openl.rules.webstudio.notification.mail;

import java.util.Map;

public interface TemplateBuilder {

    String buildMessageFromTemplate(String template, Map<String, Object> params);
}
