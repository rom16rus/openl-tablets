package org.openl.rules.rest.dto;

public class AccessEntryDTO {

    private final Long id;
    private final String accessLevel;
    private final SecurityObjectDTO securityObject;

    public AccessEntryDTO(Long id, String accessLevel, SecurityObjectDTO securityObject) {
        this.id = id;
        this.accessLevel = accessLevel;
        this.securityObject = securityObject;
    }

    public Long getId() {
        return id;
    }

    public String getAccessLevel() {
        return accessLevel;
    }

    public SecurityObjectDTO getSecurityObject() {
        return securityObject;
    }
}
