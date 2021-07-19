package org.openl.rules.webstudio.service;

import java.util.List;

import org.openl.rules.security.standalone.dao.OpenLSecurityObjectDao;
import org.openl.rules.security.standalone.persistence.OpenLAccessEntry;
import org.openl.rules.security.standalone.persistence.SecurityObjectType;

public class AccessManagementService {

    private final OpenLSecurityObjectDao openLSecurityObjectDao;

    public AccessManagementService(OpenLSecurityObjectDao openLSecurityObjectDao) {
        this.openLSecurityObjectDao = openLSecurityObjectDao;
    }

    public List<OpenLAccessEntry> getObjectAccessRights(SecurityObjectType type, String name) {
        return openLSecurityObjectDao.getObjectAccessRights(type, name);
    }

}
