package org.openl.rules.rest.dto;

public class UserAccessDTO extends AccessEntryDTO {

    private final String userName;

    public UserAccessDTO(Long id, String accessLevel, String userName, SecurityObjectDTO securityObject) {
        super(id, accessLevel, securityObject);
        this.userName = userName;
    }

    public String getUserName() {
        return userName;
    }

}
