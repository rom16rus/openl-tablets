package org.openl.rules.webstudio.service;

import org.openl.rules.security.standalone.dao.UserDao;
import org.openl.rules.security.standalone.persistence.OpenLUser;

/**
 * Creates a user for single user mode.
 */
public class SingleUserModeInit {
    public SingleUserModeInit(UserDao userDao, String userName) {
        if (userDao.getUserByName(userName) == null) {
            OpenLUser persistOpenLUser = new OpenLUser();
            persistOpenLUser.setLoginName(userName);
            userDao.save(persistOpenLUser);
        }
    }
}
