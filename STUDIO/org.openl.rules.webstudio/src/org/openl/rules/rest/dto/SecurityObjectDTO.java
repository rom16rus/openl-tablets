package org.openl.rules.rest.dto;

import java.util.ArrayList;
import java.util.List;

public class SecurityObjectDTO {
    private Long id;
    private String objectType;
    private String name;
    private List<UserAccessDTO> userAccessList = new ArrayList<>();
    private List<GroupAccessDTO> groupAccessList = new ArrayList<>();

    public SecurityObjectDTO() {
    }

    public SecurityObjectDTO(Long id, String objectType, String name, List<UserAccessDTO> userAccessList, List<GroupAccessDTO> groupAccessList) {
        this.id = id;
        this.objectType = objectType;
        this.name = name;
        this.userAccessList = userAccessList;
        this.groupAccessList = groupAccessList;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<UserAccessDTO> getUserAccessList() {
        return userAccessList;
    }

    public void setUserAccessList(List<UserAccessDTO> userAccessList) {
        this.userAccessList = userAccessList;
    }

    public List<GroupAccessDTO> getGroupAccessList() {
        return groupAccessList;
    }

    public void setGroupAccessList(List<GroupAccessDTO> groupAccessList) {
        this.groupAccessList = groupAccessList;
    }
}
