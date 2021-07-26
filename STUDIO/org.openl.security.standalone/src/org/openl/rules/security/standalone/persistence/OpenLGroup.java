package org.openl.rules.security.standalone.persistence;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Convert;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.OneToMany;
import javax.persistence.SequenceGenerator;
import javax.persistence.Table;

import org.hibernate.annotations.Type;
import org.openl.rules.security.standalone.converter.BooleanToStringConverter;

/**
 * Group.
 *
 * @author Andrey Naumenko
 */
@Entity
@Table(name = "OpenL_Groups")
public class OpenLGroup implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private String name;
    private String description;
    private Set<OpenLGroup2Group> includedGroupLinks = new HashSet<>();
    private Set<OpenLGroup2Group> parentGroupLinks = new HashSet<>();
    private Boolean isAdministrator;
    private Boolean isExternal;
    private Set<OpenLAccessEntry> accessEntries = new HashSet<>();

    /**
     * Description of group.
     *
     * @return description
     */
    @Column(length = 200, name = "description")
    public String getDescription() {
        return description;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "OpenL_Groups_ID_SEQ")
    @SequenceGenerator(sequenceName = "OpenL_Groups_ID_SEQ", name = "OpenL_Groups_ID_SEQ")
    @Column(name = "id")
    @Type(type = "java.lang.Long")
    public Long getId() {
        return id;
    }

    protected void setId(Long id) {
        this.id = id;
    }

    /**
     * Included groups.
     *
     * @return
     */
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "groupId")
    public Set<OpenLGroup2Group> getIncludedGroupLinks() {
        return includedGroupLinks;
    }

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "includedGroupId")
    public Set<OpenLGroup2Group> getParentGroupLinks() {
        return parentGroupLinks;
    }

    /**
     * Group name.
     *
     * @return
     */
    @Column(length = 40, name = "groupName", unique = true, nullable = false)
    public String getName() {
        return name;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setIncludedGroupLinks(Set<OpenLGroup2Group> includedOpenLGroups) {
        this.includedGroupLinks = includedOpenLGroups;
    }

    public void addIncludedGroup(OpenLGroup openLGroup) {
        OpenLGroup2Group openLGroup2Group = new OpenLGroup2Group(this, openLGroup, 1L);
        includedGroupLinks.add(openLGroup2Group);
        openLGroup.getParentGroupLinks().add(openLGroup2Group);
    }

    public void setParentGroupLinks(Set<OpenLGroup2Group> parentOpenLGroups) {
        this.parentGroupLinks = parentOpenLGroups;
    }

    public void setName(String name) {
        this.name = name;
    }

    @Column(name = "isAdmin", length = 1)
    @Convert(converter = BooleanToStringConverter.class)
    public Boolean getAdministrator() {
        return isAdministrator;
    }

    public void setAdministrator(Boolean administrator) {
        isAdministrator = administrator;
    }

    @Column(name = "isExternal", length = 1)
    @Convert(converter = BooleanToStringConverter.class)
    public Boolean getExternal() {
        return isExternal;
    }

    public void setExternal(Boolean external) {
        isExternal = external;
    }

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, targetEntity = OpenLGroupAccessEntry.class)
    @JoinColumn(name = "groupId")
    public Set<OpenLAccessEntry> getAccessEntries() {
        return accessEntries;
    }

    public void setAccessEntries(Set<OpenLAccessEntry> accessEntries) {
        this.accessEntries = accessEntries;
    }

    public void addAccessEntry(OpenLGroupAccessEntry openLGroupAccessEntry) {
        openLGroupAccessEntry.setGroup(this);
        accessEntries.add(openLGroupAccessEntry);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        OpenLGroup openLGroup = (OpenLGroup) o;

        return Objects.equals(id, openLGroup.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Group{id=" + id + '}';
    }
}
