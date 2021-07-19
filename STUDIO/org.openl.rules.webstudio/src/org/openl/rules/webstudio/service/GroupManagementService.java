package org.openl.rules.webstudio.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.openl.rules.security.Group;
import org.openl.rules.security.standalone.dao.GroupDao;
import org.openl.rules.security.standalone.persistence.OpenLGroup;

/**
 * @author Andrei Astrouski
 */
public class GroupManagementService {

    private final GroupDao groupDao;

    public GroupManagementService(GroupDao groupDao) {
        this.groupDao = groupDao;
    }

    public List<Group> getGroups() {
        List<OpenLGroup> openLGroups = groupDao.getAllGroups();
        List<Group> resultGroups = new ArrayList<>();

        for (OpenLGroup openLGroup : openLGroups) {
            resultGroups.add(PrivilegesEvaluator.wrap(openLGroup));
        }

        return resultGroups;
    }

    public Group getGroupByName(String name) {
        OpenLGroup openLGroup = groupDao.getGroupByName(name);
        if (openLGroup != null) {
            return PrivilegesEvaluator.wrap(openLGroup);
        }
        return null;
    }

    public boolean isGroupExist(String name) {
        return groupDao.getGroupByName(name) != null;
    }

    public void addGroup(String name, String description, boolean isAdministrator) {
        OpenLGroup persistOpenLGroup = new OpenLGroup();
        persistOpenLGroup.setName(name);
        persistOpenLGroup.setDescription(description);
        persistOpenLGroup.setAdministrator(isAdministrator);
        groupDao.save(persistOpenLGroup);
    }

    public void updateGroup(String name, String newName, String description) {
        OpenLGroup persistOpenLGroup = groupDao.getGroupByName(name);
        persistOpenLGroup.setName(newName);
        persistOpenLGroup.setDescription(description);
        groupDao.update(persistOpenLGroup);
    }

    public void updateGroup(String name, Set<String> groups, Set<String> privileges) {
        OpenLGroup persistOpenLGroup = groupDao.getGroupByName(name);

        Set<OpenLGroup> includedOpenLGroups = new HashSet<>();
        for (String group : groups) {
            OpenLGroup includedOpenLGroup = groupDao.getGroupByName(group);
            if (!persistOpenLGroup.equals(includedOpenLGroup)) {
                // Persisting group should not include itself
                includedOpenLGroups.add(includedOpenLGroup);
            }
        }

        persistOpenLGroup.setIncludedGroups(!includedOpenLGroups.isEmpty() ? includedOpenLGroups : null);
        // persistOpenLGroup.setPrivileges(privileges);

        groupDao.update(persistOpenLGroup);
    }

    public void deleteGroup(String name) {
        groupDao.deleteGroupByName(name);
    }
}
