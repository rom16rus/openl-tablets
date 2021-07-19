package org.openl.rules.security.standalone.dao;

import java.util.List;

import org.openl.rules.security.standalone.persistence.OpenLUser;
import org.openl.rules.security.standalone.persistence.OpenLUserAccessEntry;

/**
 * User dao.
 *
 * @author Andrey Naumenko
 */
public interface UserDao extends Dao<OpenLUser> {
    /**
     * Return User by name or <code>null</code> if no such User.
     *
     * @param name user name
     *
     * @return User or <code>null</code>.
     */
    OpenLUser getUserByName(String name);

    void deleteUserByName(String name);

    List<OpenLUser> getAllUsers();

    List<OpenLUserAccessEntry> getUserAccessRights(String loginName);

}
