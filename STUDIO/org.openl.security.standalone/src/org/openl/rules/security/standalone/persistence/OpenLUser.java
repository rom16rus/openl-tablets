package org.openl.rules.security.standalone.persistence;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.ManyToMany;
import javax.persistence.OneToMany;
import javax.persistence.Table;

/**
 * This class contains information about application user.
 *
 * @author Andrey Naumenko
 */
@Entity
@Table(name = "OpenL_Users") // "USER" is a reserved word in SQL92/SQL99
public class OpenLUser implements Serializable {
    private static final long serialVersionUID = 1L;
    private String loginName;
    private String passwordHash;
    private Set<OpenLGroup> openLGroups;
    private String firstName;
    private String surname;
    private Set<OpenLAccessEntry> accessEntries = new HashSet<>();

    /**
     * First name.
     */
    @Column(name = "firstName", length = 50)
    public String getFirstName() {
        return firstName;
    }

    /**
     * User's groups.
     */
    @ManyToMany(targetEntity = OpenLGroup.class, fetch = FetchType.LAZY, cascade = javax.persistence.CascadeType.MERGE)
    @JoinTable(name = "OpenL_User2Group", joinColumns = { @JoinColumn(name = "loginName") }, inverseJoinColumns = {
            @JoinColumn(name = "groupID") })
    public Set<OpenLGroup> getGroups() {
        return openLGroups;
    }

    /**
     * Login name of user.
     */
    @Id
    @Column(name = "loginName", length = 50, nullable = false, unique = true)
    public String getLoginName() {
        return loginName;
    }

    /**
     * Password of user.
     */
    @Column(name = "password", length = 128, nullable = true)
    public String getPasswordHash() {
        return passwordHash;
    }

    /**
     * Surname.
     */
    @Column(name = "surname", length = 50)
    public String getSurname() {
        return surname;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public void setGroups(Set<OpenLGroup> openLGroups) {
        this.openLGroups = openLGroups;
    }

    public void setLoginName(String loginName) {
        this.loginName = loginName;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public void setSurname(String surname) {
        this.surname = surname;
    }

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, targetEntity = OpenLUserAccessEntry.class)
    @JoinColumn(name = "loginName")
    public Set<OpenLAccessEntry> getAccessEntries() {
        return accessEntries;
    }

    public void setAccessEntries(Set<OpenLAccessEntry> accessEntries) {
        this.accessEntries = accessEntries;
    }

    public void addAccessEntry(OpenLUserAccessEntry openLGroupAccessEntry) {
        openLGroupAccessEntry.setUser(this);
        accessEntries.add(openLGroupAccessEntry);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        OpenLUser openLUser = (OpenLUser) o;

        return Objects.equals(loginName, openLUser.loginName);
    }

    @Override
    public int hashCode() {
        return loginName != null ? loginName.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "User{loginName=" + loginName + '}';
    }
}
