package org.openl.security.standalone;

import org.openl.rules.security.standalone.persistence.OpenLGroup;
import org.openl.rules.security.standalone.persistence.OpenLSecurityObject;
import org.openl.rules.security.standalone.persistence.OpenLUser;
import org.openl.rules.security.standalone.persistence.SecurityObjectType;

public class TestHelper {

    public static final String MY_LOGIN = "myLogin";
    public static final String FIRST_NAME = "John";
    public static final String SURNAME = "Doe";
    public static final String PASSWORD_HASH = "1";

    public static final String GROUP_NAME = "Test";
    public static final String GROUP_DESCRIPTION = "Test description";

    private static final String SUB_GROUP_NAME = "Subgroup test";
    private static final String SUB_GROUP_DESCRIPTION = "Subgroup description";

    public static OpenLUser stubUser() {
        OpenLUser openLUser = new OpenLUser();
        openLUser.setLoginName(MY_LOGIN);
        openLUser.setFirstName(FIRST_NAME);
        openLUser.setSurname(SURNAME);
        openLUser.setPasswordHash(PASSWORD_HASH);
        return openLUser;
    }

    public static OpenLGroup stubGroup() {
        OpenLGroup openLGroup = new OpenLGroup();
        openLGroup.setName(GROUP_NAME);
        openLGroup.setDescription(GROUP_DESCRIPTION);
        openLGroup.setAdministrator(true);
        openLGroup.setExternal(true);
        return openLGroup;
    }

    public static OpenLGroup stubSubGroup() {
        OpenLGroup openLGroup = new OpenLGroup();
        openLGroup.setName(SUB_GROUP_NAME);
        openLGroup.setDescription(SUB_GROUP_DESCRIPTION);
        openLGroup.setAdministrator(false);
        openLGroup.setExternal(true);
        return openLGroup;
    }

    public static OpenLSecurityObject stubProjectSecurityObject() {
        OpenLSecurityObject openLSecurityObject = new OpenLSecurityObject();
        openLSecurityObject.setName("Test Project object");
        openLSecurityObject.setType(SecurityObjectType.PROJECT);
        return openLSecurityObject;
    }

    public static OpenLSecurityObject stubModuleSecurityObject() {
        OpenLSecurityObject openLSecurityObject = new OpenLSecurityObject();
        openLSecurityObject.setName("Test Module object");
        openLSecurityObject.setType(SecurityObjectType.MODULE);
        return openLSecurityObject;
    }

    public static OpenLSecurityObject stubRepositorySecurityObject() {
        OpenLSecurityObject openLSecurityObject = new OpenLSecurityObject();
        openLSecurityObject.setName("Test Repository object");
        openLSecurityObject.setType(SecurityObjectType.REPOSITORY);
        return openLSecurityObject;
    }
}
