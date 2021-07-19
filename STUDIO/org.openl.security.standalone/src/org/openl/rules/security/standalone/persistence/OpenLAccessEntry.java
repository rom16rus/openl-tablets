package org.openl.rules.security.standalone.persistence;

import java.io.Serializable;
import java.util.Objects;

import javax.persistence.Column;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.MappedSuperclass;

import org.hibernate.annotations.Type;

@MappedSuperclass
public abstract class OpenLAccessEntry implements Serializable {

    private Long id;
    private AccessLevel accessLevel;
    private OpenLSecurityObject openLSecurityObject;

    @Id
    @Column(name = "id")
    @GeneratedValue(strategy = GenerationType.IDENTITY, generator = "OpenL_Access_Entry_ID_SEQ")
    @Type(type = "java.lang.Long")
    public Long getId() {
        return id;
    }

    protected void setId(Long id) {
        this.id = id;
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "accessLevel", nullable = false, length = 50)
    public AccessLevel getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(AccessLevel accessLevel) {
        this.accessLevel = accessLevel;
    }

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "objectId", nullable = false)
    public OpenLSecurityObject getOpenLSecurityObject() {
        return openLSecurityObject;
    }

    public void setOpenLSecurityObject(OpenLSecurityObject openLSecurityObject) {
        this.openLSecurityObject = openLSecurityObject;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OpenLAccessEntry that = (OpenLAccessEntry) o;
        return Objects.equals(id, that.id) && accessLevel == that.accessLevel && Objects.equals(openLSecurityObject,
            that.openLSecurityObject);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, accessLevel, openLSecurityObject);
    }
}
