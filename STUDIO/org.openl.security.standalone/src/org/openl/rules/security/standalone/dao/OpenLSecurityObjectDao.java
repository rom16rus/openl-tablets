package org.openl.rules.security.standalone.dao;

import java.util.List;

import org.openl.rules.security.standalone.persistence.OpenLAccessEntry;
import org.openl.rules.security.standalone.persistence.OpenLSecurityObject;
import org.openl.rules.security.standalone.persistence.SecurityObjectType;

public interface OpenLSecurityObjectDao extends Dao<OpenLSecurityObject> {

    List<OpenLSecurityObject> getByType(SecurityObjectType securityObjectType);

    List<OpenLSecurityObject> findAll();

    List<OpenLSecurityObject> findByNameAndType(String name, SecurityObjectType securityObjectType);

    List<OpenLAccessEntry> getObjectAccessRights(SecurityObjectType type, String name);

}
