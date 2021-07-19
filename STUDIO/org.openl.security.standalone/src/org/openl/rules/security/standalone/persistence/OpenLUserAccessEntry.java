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
@Table(name = "OpenL_User_Access_Entry")
@SequenceGenerator(name = "OpenL_Access_Entry_ID_SEQ", sequenceName = "OpenL_User_Access_Entry_ID_SEQ")
public class OpenLUserAccessEntry extends OpenLAccessEntry implements Serializable {

    private OpenLUser user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "loginname", nullable = false)
    public OpenLUser getUser() {
        return user;
    }

    public void setUser(OpenLUser user) {
        this.user = user;
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
        OpenLUserAccessEntry that = (OpenLUserAccessEntry) o;
        return Objects.equals(user, that.user);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
