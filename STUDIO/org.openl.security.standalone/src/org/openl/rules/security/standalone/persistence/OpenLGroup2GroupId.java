package org.openl.rules.security.standalone.persistence;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.Embeddable;

@Embeddable
public class OpenLGroup2GroupId implements Serializable {

    private Long groupId;
    private Long includedGroupId;

    public OpenLGroup2GroupId() {
    }

    public OpenLGroup2GroupId(Long groupId, Long includedGroupId) {
        this.groupId = groupId;
        this.includedGroupId = includedGroupId;
    }

    @Column(name = "groupId")
    public Long getGroupId() {
        return groupId;
    }

    public void setGroupId(Long groupId) {
        this.groupId = groupId;
    }

    @Column(name = "includedGroupId")
    public Long getIncludedGroupId() {
        return includedGroupId;
    }

    public void setIncludedGroupId(Long includedGroupId) {
        this.includedGroupId = includedGroupId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OpenLGroup2GroupId that = (OpenLGroup2GroupId) o;
        return Objects.equals(groupId, that.groupId) && Objects.equals(includedGroupId, that.includedGroupId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(groupId, includedGroupId);
    }
}
