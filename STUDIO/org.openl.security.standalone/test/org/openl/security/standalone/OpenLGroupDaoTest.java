package org.openl.security.standalone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.openl.security.standalone.TestHelper.GROUP_DESCRIPTION;
import static org.openl.security.standalone.TestHelper.GROUP_NAME;
import static org.openl.security.standalone.TestHelper.stubGroup;


import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.rules.security.standalone.dao.GroupDao;
import org.openl.rules.security.standalone.persistence.OpenLGroup;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/META-INF/spring/applicationContext.xml" })
public class OpenLGroupDaoTest {

    private static final String GROUP_NAME_MODIFIED = "Test Group";

    private static final String GROUP_DESCRIPTION_MODIFIED = "Test description Test";

    @Autowired
    private GroupDao groupDao;

    @Test
    public void testSaveGroupWithoutAccessEntries() {
        groupDao.save(stubGroup());
        final OpenLGroup group = groupDao.getGroupByName(GROUP_NAME);
        assertNotNull(group);
        assertEquals(GROUP_NAME, group.getName());
        assertEquals(GROUP_DESCRIPTION, group.getDescription());
        assertTrue(group.getAdministrator());
        assertTrue(group.getExternal());

        group.setName(GROUP_NAME_MODIFIED);
        group.setDescription(GROUP_DESCRIPTION_MODIFIED);
        group.setAdministrator(false);
        group.setExternal(false);
        groupDao.update(group);

        final OpenLGroup modifiedGroup = groupDao.getGroupByName(GROUP_NAME_MODIFIED);
        assertNotNull(modifiedGroup);
        assertEquals(GROUP_NAME_MODIFIED, modifiedGroup.getName());
        assertEquals(GROUP_DESCRIPTION_MODIFIED, modifiedGroup.getDescription());
        assertFalse(modifiedGroup.getExternal());
        assertFalse(modifiedGroup.getAdministrator());

        groupDao.deleteGroupByName(GROUP_NAME_MODIFIED);
        assertNull(groupDao.getGroupByName(GROUP_NAME_MODIFIED));

    }
}
