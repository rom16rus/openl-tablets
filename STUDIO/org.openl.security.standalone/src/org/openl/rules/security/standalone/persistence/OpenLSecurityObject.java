package org.openl.rules.security.standalone.persistence;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.annotations.Type;

@Entity
@Table(name = "OpenL_Security_Objects")
public class OpenLSecurityObject implements Serializable {

    private Long id;
    private SecurityObjectType type;
    /**
     * for modules - branchName+ name + projectPath for projects - projectPath + repositoryID for repositories - id
     *
     */
    private String name;
    private Set<OpenLUserAccessEntry> userAccessEntries = new HashSet<>();
    private Set<OpenLGroupAccessEntry> groupAccessEntries = new HashSet<>();

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "OpenL_Security_Object_ID_SEQ")
    @SequenceGenerator(sequenceName = "OpenL_Security_Object_ID_SEQ", name = "OpenL_Security_Object_ID_SEQ")
    @Column(name = "id")
    @Type(type = "java.lang.Long")
    public Long getId() {
        return id;
    }

    protected void setId(Long id) {
        this.id = id;
    }

    @Enumerated(EnumType.STRING)
    @Column(name = "objectType", nullable = false, length = 50)
    public SecurityObjectType getType() {
        return type;
    }

    public void setType(SecurityObjectType type) {
        this.type = type;
    }

    @Column(name = "objectName", nullable = false, length = 100)
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "objectId")
    public Set<OpenLUserAccessEntry> getUserAccessEntries() {
        return userAccessEntries;
    }

    public void setUserAccessEntries(Set<OpenLUserAccessEntry> userAccessEntries) {
        this.userAccessEntries = userAccessEntries;
    }

    public void addUserAccessEntry(OpenLUserAccessEntry userAccessEntry) {
        userAccessEntry.setOpenLSecurityObject(this);
        userAccessEntries.add(userAccessEntry);
    }

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "objectId")
    public Set<OpenLGroupAccessEntry> getGroupAccessEntries() {
        return groupAccessEntries;
    }

    public void setGroupAccessEntries(Set<OpenLGroupAccessEntry> groupAccessEntries) {
        this.groupAccessEntries = groupAccessEntries;
    }

    public void addGroupAccessEntry(OpenLGroupAccessEntry groupAccessEntry) {
        groupAccessEntry.setOpenLSecurityObject(this);
        groupAccessEntries.add(groupAccessEntry);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        OpenLSecurityObject that = (OpenLSecurityObject) o;
        return Objects.equals(id, that.id) && type == that.type && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, type, name);
    }
}
