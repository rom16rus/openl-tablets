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
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
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
    private Set<OpenLGroup> includedOpenLGroups;
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
    @ManyToMany(targetEntity = OpenLGroup.class, fetch = FetchType.EAGER, cascade = javax.persistence.CascadeType.MERGE)
    @JoinTable(name = "OpenL_Group2Group", joinColumns = { @JoinColumn(name = "groupID") }, inverseJoinColumns = {
            @JoinColumn(name = "includedGroupID") })
    public Set<OpenLGroup> getIncludedGroups() {
        return includedOpenLGroups;
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

    public void setIncludedGroups(Set<OpenLGroup> includedOpenLGroups) {
        this.includedOpenLGroups = includedOpenLGroups;
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

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, targetEntity = OpenLGroupAccessEntry.class)
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
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

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
