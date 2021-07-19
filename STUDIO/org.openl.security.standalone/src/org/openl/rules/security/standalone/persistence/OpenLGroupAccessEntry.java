package org.openl.rules.security.standalone.persistence;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

@Entity
@Table(name = "OpenL_Group_Access_Entry")
@SequenceGenerator(name = "OpenL_Access_Entry_ID_SEQ", sequenceName = "OpenL_Group_Access_Entry_ID_SEQ")
public class OpenLGroupAccessEntry extends OpenLAccessEntry implements Serializable {

    private OpenLGroup group;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "groupId", nullable = false)
    public OpenLGroup getGroup() {
        return group;
    }

    public void setGroup(OpenLGroup group) {
        this.group = group;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }
        OpenLGroupAccessEntry that = (OpenLGroupAccessEntry) o;
        return Objects.equals(group, that.group);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
