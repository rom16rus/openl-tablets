package org.openl.rules.security.standalone.persistence;

import org.openl.rules.security.Privilege;

public enum AccessLevel implements Privilege {

    MANAGER("Manager", (byte) 3),
    EDITOR("Editor", (byte) 2),
    DEPLOYER("Deployer", (byte) 1),
    VIEWER("Viewer", (byte) 0),
    FORBIDDEN("Forbidden", (byte) 127);

    private final String displayName;
    private final byte priority;

    AccessLevel(String displayName, byte priority) {
        this.displayName = displayName;
        this.priority = priority;
    }

    @Override
    public String getName() {
        return name();
    }

    @Override
    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getAuthority() {
        return name();
    }

    public byte getPriority() {
        return priority;
    }

}
