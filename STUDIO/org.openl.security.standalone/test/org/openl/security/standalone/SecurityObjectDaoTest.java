package org.openl.security.standalone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.openl.security.standalone.TestHelper.stubGroup;
import static org.openl.security.standalone.TestHelper.stubModuleSecurityObject;
import static org.openl.security.standalone.TestHelper.stubProjectSecurityObject;
import static org.openl.security.standalone.TestHelper.stubRepositorySecurityObject;
import static org.openl.security.standalone.TestHelper.stubSubGroup;
import static org.openl.security.standalone.TestHelper.stubUser;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.rules.security.standalone.dao.GroupDao;
import org.openl.rules.security.standalone.dao.OpenLSecurityObjectDao;
import org.openl.rules.security.standalone.dao.UserDao;
import org.openl.rules.security.standalone.persistence.AccessLevel;
import org.openl.rules.security.standalone.persistence.OpenLAccessEntry;
import org.openl.rules.security.standalone.persistence.OpenLGroup;
import org.openl.rules.security.standalone.persistence.OpenLGroupAccessEntry;
import org.openl.rules.security.standalone.persistence.OpenLSecurityObject;
import org.openl.rules.security.standalone.persistence.OpenLUser;
import org.openl.rules.security.standalone.persistence.OpenLUserAccessEntry;
import org.openl.rules.security.standalone.persistence.SecurityObjectType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/META-INF/spring/applicationContext.xml" })
public class SecurityObjectDaoTest {

    @Autowired
    private UserDao userDao;

    @Autowired
    private GroupDao groupDao;

    @Autowired
    private OpenLSecurityObjectDao securityObjectDao;

    public SecurityObjectDaoTest() {
    }

    @Test
    public void testAccessLevelEntries() {
        final OpenLSecurityObject project = stubProjectSecurityObject();
        securityObjectDao.save(project);

        final OpenLSecurityObject module = stubModuleSecurityObject();
        securityObjectDao.save(module);

        final OpenLGroupAccessEntry groupAccessEntry = new OpenLGroupAccessEntry();
        groupAccessEntry.setAccessLevel(AccessLevel.EDITOR);
        groupAccessEntry.setOpenLSecurityObject(project);

        final OpenLUserAccessEntry userAccessEntry = new OpenLUserAccessEntry();
        userAccessEntry.setAccessLevel(AccessLevel.FORBIDDEN);
        userAccessEntry.setOpenLSecurityObject(module);

        OpenLGroup subGroup = stubSubGroup();
        groupDao.save(subGroup);

        OpenLGroup group = stubGroup();
        final Set<OpenLGroup> subGroupSet = Collections.singleton(subGroup);
        groupDao.save(group);

        group.addIncludedGroup(subGroup);
        group.addAccessEntry(groupAccessEntry);
        groupDao.update(group);

        final OpenLUser openLUser = stubUser();
        openLUser.setGroups(subGroupSet);
        userDao.save(openLUser);

        openLUser.addAccessEntry(userAccessEntry);
        userDao.update(openLUser);

        final List<OpenLAccessEntry> accessRightsOnProject = securityObjectDao
            .getObjectAccessRights(SecurityObjectType.PROJECT, project.getName());
        assertFalse(accessRightsOnProject.isEmpty());

        final Optional<OpenLAccessEntry> accessEntryOptional = accessRightsOnProject.stream().findFirst();
        final OpenLAccessEntry foundGroupAccessEntry = accessEntryOptional.get();
        assertTrue(foundGroupAccessEntry instanceof OpenLGroupAccessEntry);

        final OpenLGroup associatedGroup = ((OpenLGroupAccessEntry) foundGroupAccessEntry).getGroup();
        assertEquals(group, associatedGroup);

        final OpenLSecurityObject openLSecurityObject = foundGroupAccessEntry.getOpenLSecurityObject();
        assertEquals(project, openLSecurityObject);
        assertEquals(AccessLevel.EDITOR, foundGroupAccessEntry.getAccessLevel());

        final List<OpenLAccessEntry> accessRightsOnModule = securityObjectDao
            .getObjectAccessRights(SecurityObjectType.MODULE, module.getName());
        assertFalse(accessRightsOnModule.isEmpty());
        final OpenLAccessEntry openLUserAccessEntry = accessRightsOnModule.stream().findFirst().get();
        assertTrue(openLUserAccessEntry instanceof OpenLUserAccessEntry);
        assertEquals(module, openLUserAccessEntry.getOpenLSecurityObject());
        assertEquals(AccessLevel.FORBIDDEN, openLUserAccessEntry.getAccessLevel());
        assertEquals(openLUser, ((OpenLUserAccessEntry) openLUserAccessEntry).getUser());

    }

    @Test
    public void testSaveObject() {
        final OpenLSecurityObject repository = stubRepositorySecurityObject();

        OpenLUser openLUser = new OpenLUser();
        openLUser.setLoginName("TestSave");
        openLUser.setFirstName("F");
        openLUser.setSurname("S");
        openLUser.setPasswordHash("123");
        userDao.save(openLUser);

        OpenLGroup openLGroup = new OpenLGroup();
        openLGroup.setName("TestGroupSave");
        openLGroup.setDescription("");
        groupDao.save(openLGroup);

        OpenLUserAccessEntry openLUserAccessEntry = new OpenLUserAccessEntry();
        openLUserAccessEntry.setUser(openLUser);
        openLUserAccessEntry.setAccessLevel(AccessLevel.FORBIDDEN);

        OpenLGroupAccessEntry openLGroupAccessEntry = new OpenLGroupAccessEntry();
        openLGroupAccessEntry.setGroup(openLGroup);
        openLGroupAccessEntry.setAccessLevel(AccessLevel.VIEWER);

        repository.addUserAccessEntry(openLUserAccessEntry);
        repository.addGroupAccessEntry(openLGroupAccessEntry);

        securityObjectDao.save(repository);

        final List<OpenLAccessEntry> accessRightsOnRepository = securityObjectDao
            .getObjectAccessRights(SecurityObjectType.REPOSITORY, repository.getName());
        assertEquals(2, accessRightsOnRepository.size());
    }

}
