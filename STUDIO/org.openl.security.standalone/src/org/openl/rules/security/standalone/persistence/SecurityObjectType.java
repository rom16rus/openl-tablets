package org.openl.rules.security.standalone.persistence;

/**
 * Security object types. WEBSTUDIO type by default, gives an access for all the repositories, projects, modules etc.
 */
public enum SecurityObjectType {
    WEBSTUDIO((byte) 1),
    REPOSITORY((byte) 2),
    PROJECT((byte) 3),
    MODULE((byte) 4);

    private final byte priority;

    SecurityObjectType(byte priority) {
        this.priority = priority;
    }

    public byte getPriority() {
        return priority;
    }
}
