package org.openl.rules.security.standalone;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.openl.rules.security.Privilege;
import org.openl.rules.security.SimpleGroup;
import org.openl.rules.security.standalone.persistence.AccessLevel;
import org.openl.rules.security.standalone.persistence.OpenLAccessEntry;
import org.openl.rules.security.standalone.persistence.OpenLGroup;
import org.openl.rules.security.standalone.persistence.OpenLGroup2Group;
import org.openl.rules.security.standalone.persistence.OpenLUser;

public final class PrivilegesEvaluator {
    private PrivilegesEvaluator() {
    }

    public static Collection<Privilege> createPrivileges(OpenLUser user) {
        Collection<Privilege> grantedList = new ArrayList<>();

        Set<OpenLGroup> visitedGroups = new HashSet<>();
        Set<OpenLGroup> groups = user.getGroups();
        for (OpenLGroup openLGroup : groups) {
            Collection<Privilege> privileges = createPrivileges(openLGroup, visitedGroups);
            grantedList.add(new SimpleGroup(openLGroup.getName(),
                openLGroup.getDescription(),
                privileges,
                openLGroup.getAdministrator()));
        }
        return grantedList;
    }

    public static Collection<OpenLAccessEntry> createAccessEntries(OpenLUser openLUser) {
        Set<OpenLAccessEntry> grantedList = new HashSet<>();

        Set<OpenLGroup> visitedOpenLGroups = new HashSet<>();
        Set<OpenLGroup> openLGroups = openLUser.getGroups();

        for (OpenLGroup openLGroup : openLGroups) {
            Collection<OpenLAccessEntry> accessEntries = createGroupAccessEntries(openLGroup,
                visitedOpenLGroups,
                true,
                true);
            grantedList.addAll(accessEntries);
        }

        grantedList.addAll(openLUser.getAccessEntries());
        return grantedList;
    }

    public static Collection<OpenLAccessEntry> createGroupAccessEntries(OpenLGroup openLGroup,
            Set<OpenLGroup> visitedOpenLGroups,
            boolean visitChild,
            boolean visitParents) {
        visitedOpenLGroups.add(openLGroup);
        Collection<OpenLAccessEntry> accessEntries = new ArrayList<>();

        if (visitChild) {
            for (OpenLGroup2Group includedGroupLink : openLGroup.getIncludedGroupLinks()) {
                OpenLGroup persistGroup = includedGroupLink.getIncludedGroup();
                if (!visitedOpenLGroups.contains(persistGroup)) {
                    Collection<OpenLAccessEntry> includedGroupAccessEntries = createGroupAccessEntries(persistGroup,
                        visitedOpenLGroups,
                        true,
                        false);
                    accessEntries.addAll(includedGroupAccessEntries);
                }
            }
        }

        if (visitParents) {
            for (OpenLGroup2Group parentGroupLink : openLGroup.getParentGroupLinks()) {
                OpenLGroup persistGroup = parentGroupLink.getParentGroup();
                if (!visitedOpenLGroups.contains(persistGroup)) {
                    Collection<OpenLAccessEntry> groupAccessEntries = createGroupAccessEntries(persistGroup,
                        visitedOpenLGroups,
                        false,
                        true);
                    accessEntries.addAll(groupAccessEntries);
                }
            }
        }

        accessEntries.addAll(openLGroup.getAccessEntries());
        return accessEntries;
    }

    public static SimpleGroup wrap(OpenLGroup openLGroup) {
        Collection<Privilege> privileges = PrivilegesEvaluator.createPrivileges(openLGroup, new HashSet<>());
        return new SimpleGroup(openLGroup.getName(),
            openLGroup.getDescription(),
            privileges,
            openLGroup.getAdministrator());
    }

    private static Collection<Privilege> createPrivileges(OpenLGroup openLGroup, Set<OpenLGroup> visitedOpenLGroups) {
        visitedOpenLGroups.add(openLGroup);
        Collection<Privilege> grantedList = new ArrayList<>();

        Set<OpenLGroup2Group> groups = openLGroup.getIncludedGroupLinks();
        for (OpenLGroup2Group includedGroup : groups) {
            OpenLGroup persistGroup = includedGroup.getIncludedGroup();
            if (!visitedOpenLGroups.contains(persistGroup)) {
                Collection<Privilege> privileges = createPrivileges(persistGroup, visitedOpenLGroups);
                SimpleGroup simpleGroup = new SimpleGroup(persistGroup.getName(),
                    persistGroup.getDescription(),
                    privileges,
                    openLGroup.getAdministrator());
                grantedList.add(simpleGroup);
            }
        }

        Set<OpenLAccessEntry> privileges = openLGroup.getAccessEntries();
        for (OpenLAccessEntry privilege : privileges) {
            final AccessLevel accessLevel = privilege.getAccessLevel();
            grantedList.add(accessLevel);
        }
        return grantedList;
    }
}
