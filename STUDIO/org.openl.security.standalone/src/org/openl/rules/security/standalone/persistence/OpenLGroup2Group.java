package org.openl.rules.security.standalone.persistence;

import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.EmbeddedId;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

@Entity
@Table(name = "openl_group2group")
public class OpenLGroup2Group {

    private OpenLGroup2GroupId id;
    private OpenLGroup parentGroup;
    private OpenLGroup includedGroup;
    private Long level;

    public OpenLGroup2Group() {
    }

    public OpenLGroup2Group(OpenLGroup parentGroup, OpenLGroup includedGroup, Long level) {
        this.id = new OpenLGroup2GroupId(parentGroup.getId(), includedGroup.getId());
        this.parentGroup = parentGroup;
        this.includedGroup = includedGroup;
        this.level = level;
    }

    @EmbeddedId
    public OpenLGroup2GroupId getId() {
        return id;
    }

    public void setId(OpenLGroup2GroupId id) {
        this.id = id;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "groupId", insertable = false, updatable = false)
    public OpenLGroup getParentGroup() {
        return parentGroup;
    }

    public void setParentGroup(OpenLGroup parentGroup) {
        this.parentGroup = parentGroup;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "includedGroupId", insertable = false, updatable = false)
    public OpenLGroup getIncludedGroup() {
        return includedGroup;
    }

    public void setIncludedGroup(OpenLGroup includedGroup) {
        this.includedGroup = includedGroup;
    }

    @Column(name = "level")
    public Long getLevel() {
        return level;
    }

    public void setLevel(Long level) {
        this.level = level;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OpenLGroup2Group that = (OpenLGroup2Group) o;
        return Objects.equals(id, that.id) && Objects.equals(level, that.level);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, level);
    }
}
