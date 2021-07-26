package org.openl.rules.webstudio.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openl.rules.security.Privilege;
import org.openl.rules.security.Privileges;
import org.openl.rules.security.SimpleGroup;
import org.openl.rules.security.SimpleUser;
import org.openl.rules.security.User;
import org.openl.rules.security.standalone.PrivilegesEvaluator;
import org.openl.rules.security.standalone.dao.GroupDao;
import org.openl.rules.security.standalone.dao.UserDao;
import org.openl.rules.security.standalone.persistence.OpenLGroup;
import org.openl.rules.security.standalone.persistence.OpenLUser;

/**
 * @author Andrei Astrouski
 */
public class UserManagementService {

    private final UserDao userDao;
    private final GroupDao groupDao;

    public UserManagementService(UserDao userDao, GroupDao groupDao) {
        this.userDao = userDao;
        this.groupDao = groupDao;
    }

    public User loadUserByUsername(String name) {
        OpenLUser openLUser = userDao.getUserByName(name);

        if (openLUser == null) {
            return null;
        }

        Collection<Privilege> privileges = PrivilegesEvaluator.createPrivileges(openLUser);
        String firstName = openLUser.getFirstName();
        String lastName = openLUser.getSurname();
        String username = openLUser.getLoginName();
        String passwordHash = openLUser.getPasswordHash();

        return new SimpleUser(firstName, lastName, username, passwordHash, privileges);
    }

    public List<User> getAllUsers() {
        List<OpenLUser> openLUsers = userDao.getAllUsers();
        List<User> resultUsers = new ArrayList<>();
        for (OpenLUser openLUser : openLUsers) {
            User resultUser = new SimpleUser(openLUser.getFirstName(),
                openLUser.getSurname(),
                openLUser.getLoginName(),
                openLUser.getPasswordHash(),
                PrivilegesEvaluator.createPrivileges(openLUser));
            resultUsers.add(resultUser);
        }
        return resultUsers;
    }

    public void addUser(String user, String firstName, String lastName, String passwordHash) {
        OpenLUser persistOpenLUser = new OpenLUser();
        persistOpenLUser.setLoginName(user);
        persistOpenLUser.setPasswordHash(passwordHash);
        persistOpenLUser.setFirstName(firstName);
        persistOpenLUser.setSurname(lastName);

        userDao.save(persistOpenLUser);
    }

    public void updateUserData(String user,
            String firstName,
            String lastName,
            String passwordHash,
            boolean updatePassword) {
        OpenLUser persistOpenLUser = userDao.getUserByName(user);
        persistOpenLUser.setFirstName(firstName);
        persistOpenLUser.setSurname(lastName);
        if (updatePassword) {
            persistOpenLUser.setPasswordHash(passwordHash);
        }
        userDao.update(persistOpenLUser);
    }

    public void updateAuthorities(String user, Set<String> authorities) {
        OpenLUser persistOpenLUser = userDao.getUserByName(user);
        Set<OpenLGroup> openLGroups = new HashSet<>();
        for (String auth : authorities) {
            openLGroups.add(groupDao.getGroupByName(auth));
        }
        persistOpenLUser.setGroups(openLGroups);

        userDao.update(persistOpenLUser);
    }

    public void updateAuthorities(final String user, final Set<String> authorities, final boolean leaveAdminGroups) {
        Set<String> fullAuthorities = new HashSet<>(authorities);
        if (leaveAdminGroups) {
            OpenLUser persistOpenLUser = userDao.getUserByName(user);
            Set<OpenLGroup> currentOpenLGroups = persistOpenLUser.getGroups();
            Set<String> currentAdminGroups = getCurrentAdminGroups(currentOpenLGroups);
            fullAuthorities.addAll(currentAdminGroups);
        }
        updateAuthorities(user, fullAuthorities);
    }

    public Set<String> getCurrentAdminGroups(final Set<OpenLGroup> openLGroups) {
        Set<String> groupNames = new HashSet<>();

        for (OpenLGroup openLGroup : openLGroups) {
            SimpleGroup simpleGroup = PrivilegesEvaluator.wrap(openLGroup);
            if (simpleGroup.hasPrivilege(Privileges.ADMIN.getAuthority())) {
                groupNames.add(openLGroup.getName());
            }
        }

        return groupNames;
    }

    public void deleteUser(String username) {
        userDao.deleteUserByName(username);
    }
}
