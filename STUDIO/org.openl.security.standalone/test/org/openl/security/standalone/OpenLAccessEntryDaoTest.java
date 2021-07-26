package org.openl.security.standalone;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.rules.security.standalone.PrivilegesEvaluator;
import org.openl.rules.security.standalone.dao.UserDao;
import org.openl.rules.security.standalone.persistence.OpenLAccessEntry;
import org.openl.rules.security.standalone.persistence.OpenLSecurityObject;
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
        final Collection<OpenLAccessEntry> accessEntries = PrivilegesEvaluator.createAccessEntries(userJohn);
        final Map<OpenLSecurityObject, List<OpenLAccessEntry>> accessMap = accessEntries.stream()
            .collect(Collectors.groupingBy(OpenLAccessEntry::getOpenLSecurityObject));
        accessMap.size();
    }

    @Test
    public void testMichaelPermissions() {
        final OpenLUser userMichael = userDao.getUserByName("Michael");
        final Collection<OpenLAccessEntry> accessEntries = PrivilegesEvaluator.createAccessEntries(userMichael);
        final Map<OpenLSecurityObject, List<OpenLAccessEntry>> accessMap = accessEntries.stream()
            .collect(Collectors.groupingBy(OpenLAccessEntry::getOpenLSecurityObject));
        accessMap.size();
    }

    @Test
    public void testAnnPermissions() {
        final OpenLUser userAnn = userDao.getUserByName("Ann");
        final Collection<OpenLAccessEntry> accessEntries = PrivilegesEvaluator.createAccessEntries(userAnn);
        final Map<OpenLSecurityObject, List<OpenLAccessEntry>> accessMap = accessEntries.stream()
            .collect(Collectors.groupingBy(OpenLAccessEntry::getOpenLSecurityObject));
        accessMap.size();
    }

}
