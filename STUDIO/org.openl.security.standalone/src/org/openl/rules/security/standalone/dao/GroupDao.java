package org.openl.rules.security.standalone.dao;

import java.util.List;

import org.openl.rules.security.standalone.persistence.OpenLGroup;

/**
 * Group dao.
 *
 * @author Andrey Naumenko
 */
public interface GroupDao extends Dao<OpenLGroup> {

    OpenLGroup getGroupByName(String name);

    void deleteGroupByName(String name);

    List<OpenLGroup> getAllGroups();
}
