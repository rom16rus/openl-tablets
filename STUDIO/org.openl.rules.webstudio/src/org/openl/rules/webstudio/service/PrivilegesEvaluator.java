package org.openl.rules.webstudio.service;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.openl.rules.security.Privilege;
import org.openl.rules.security.SimpleGroup;
import org.openl.rules.security.standalone.persistence.AccessLevel;
import org.openl.rules.security.standalone.persistence.OpenLAccessEntry;
import org.openl.rules.security.standalone.persistence.OpenLGroup;
import org.openl.rules.security.standalone.persistence.OpenLUser;

public final class PrivilegesEvaluator {
    private PrivilegesEvaluator() {
    }

    public static Collection<Privilege> createPrivileges(OpenLUser openLUser) {
        Collection<Privilege> grantedList = new ArrayList<>();

        Set<OpenLGroup> visitedOpenLGroups = new HashSet<>();
        Set<OpenLGroup> openLGroups = openLUser.getGroups();
        for (OpenLGroup openLGroup : openLGroups) {
            Collection<Privilege> privileges = createPrivileges(openLGroup, visitedOpenLGroups);
            grantedList.add(new SimpleGroup(openLGroup.getName(),
                openLGroup.getDescription(),
                privileges,
                openLGroup.getAdministrator()));
        }
        // TODO
        // add user privileges there
        // for (OpenLAccessEntry accessEntry : openLUser.getAccessEntries()) {
        // grantedList.add()
        // }
        return grantedList;
    }

    public static Collection<OpenLAccessEntry> collectAccessEntries(OpenLUser openLUser) {
        Collection<OpenLAccessEntry> accessEntries = new ArrayList<>();

        Set<OpenLGroup> visitedOpenLGroups = new HashSet<>();
//        for (OpenLGroup group : openLUser.getGroups()) {
//            Collection<OpenLAccessEntry> groupAccessEntries = collectAccessEntries(group, visitedOpenLGroups);
//            accessEntries.addAll(groupAccessEntries);
//        }

        accessEntries.addAll(openLUser.getAccessEntries());
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

        Set<OpenLGroup> groups = openLGroup.getIncludedGroups();
        for (OpenLGroup persistGroup : groups) {
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
