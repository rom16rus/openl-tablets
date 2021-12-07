package org.openl.rules.rest.model;

import java.util.Set;

import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import org.hibernate.validator.constraints.NotBlank;
import org.hibernate.validator.constraints.NotEmpty;
import org.openl.rules.rest.validation.InternalPasswordConstraint;
import org.openl.rules.rest.validation.UsernameExistsConstraint;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class UserCreateModel extends UserEditModel {

    @InternalPasswordConstraint
    private InternalPasswordModel internalPassword;

    @NotBlank
    @Size(max = 25, message = "{openl.constraints.size.max.message}")
    @Pattern.List({ @Pattern(regexp = "[^.\\s].*[^.\\s]|[^.\\s]", message = "{openl.constraints.username.2.message}"),
            @Pattern(regexp = "(.(?<![.]{2}))+", message = "{openl.constraints.username.1.message}"),
            @Pattern(regexp = "[^\\/\\\\:*?\"<>|{}~^]*", message = "{openl.constraints.username.3.message}") })
    @UsernameExistsConstraint
    @JsonIgnore(false)
    public String getUsername() {
        return super.getUsername();
    }

    public UserCreateModel setUsername(String username) {
        super.setUsername(username);
        return this;
    }

    public InternalPasswordModel getInternalPassword() {
        return internalPassword;
    }

    public UserCreateModel setInternalPassword(InternalPasswordModel internalPassword) {
        this.internalPassword = internalPassword;
        return this;
    }

    @Override
    public String getFirstName() {
        return super.getFirstName();
    }

    @Override
    public UserCreateModel setFirstName(String firstName) {
        return (UserCreateModel) super.setFirstName(firstName);
    }

    @Override
    public String getLastName() {
        return super.getLastName();
    }

    @Override
    public UserCreateModel setLastName(String lastName) {
        return (UserCreateModel) super.setLastName(lastName);
    }

    @Override
    public String getEmail() {
        return super.getEmail();
    }

    @Override
    public UserCreateModel setEmail(String email) {
        return (UserCreateModel) super.setEmail(email);
    }

    @Override
    public String getDisplayName() {
        return super.getDisplayName();
    }

    @Override
    public UserCreateModel setDisplayName(String displayName) {
        return (UserCreateModel) super.setDisplayName(displayName);
    }

    @Override
    @NotEmpty(message = "{openl.constraints.user.groups.empty.message}")
    public Set<String> getGroups() {
        return super.getGroups();
    }

    @Override
    public UserCreateModel setGroups(Set<String> groups) {
        return (UserCreateModel) super.setGroups(groups);
    }

}
