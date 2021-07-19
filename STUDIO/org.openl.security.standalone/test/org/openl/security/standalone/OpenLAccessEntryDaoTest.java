package org.openl.security.standalone;

import java.util.Set;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.rules.security.standalone.dao.UserDao;
import org.openl.rules.security.standalone.persistence.OpenLAccessEntry;
import org.openl.rules.security.standalone.persistence.OpenLGroup;
import org.openl.rules.security.standalone.persistence.OpenLUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/META-INF/spring/applicationContext.xml" })
public class OpenLAccessEntryDaoTest {

    @Autowired
    private UserDao userDao;

    /**
     * Based on the test data : User with login John has a ClassicGroup with the following permissions: WebStudio -
     * Manager level. Corporate Rating module in the testProject is FORBIDDEN
     */
    @Test
    public void testJohnPermissions() {
        final OpenLUser userJohn = userDao.getUserByName("John");
        final Set<OpenLAccessEntry> accessEntries = userJohn.getAccessEntries();
        final Set<OpenLGroup> groups = userJohn.getGroups();
        // the same as for collecting priviliges?
        // grouping the permissions by objects and link them
    }

}
