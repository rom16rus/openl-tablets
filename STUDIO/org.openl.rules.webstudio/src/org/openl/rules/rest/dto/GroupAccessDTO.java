package org.openl.rules.rest.dto;

public class GroupAccessDTO extends AccessEntryDTO {

    private final String groupName;

    public GroupAccessDTO(Long id, String accessLevel, String groupName, SecurityObjectDTO securityObject) {
        super(id, accessLevel, securityObject);
        this.groupName = groupName;
    }

    public String getGroupName() {
        return groupName;
    }
}
