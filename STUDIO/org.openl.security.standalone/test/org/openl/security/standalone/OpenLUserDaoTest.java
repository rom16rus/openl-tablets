package org.openl.security.standalone;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.openl.security.standalone.TestHelper.FIRST_NAME;
import static org.openl.security.standalone.TestHelper.MY_LOGIN;
import static org.openl.security.standalone.TestHelper.PASSWORD_HASH;
import static org.openl.security.standalone.TestHelper.SURNAME;


import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.openl.rules.security.standalone.dao.UserDao;
import org.openl.rules.security.standalone.persistence.OpenLUser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "/META-INF/spring/applicationContext.xml" })
public class OpenLUserDaoTest {

    private static final String FIRST_NAME_MODIFIED = "Andrew";

    private static final String SURNAME_MODIFIED = "OpenL";

    private static final String PASSWORD_HASH_MODIFIED = "111";

    @Autowired
    private UserDao userDao;

    @Test
    public void testSimpleUserOperations() {

        userDao.save(TestHelper.stubUser());

        final OpenLUser simpleUser = userDao.getUserByName(MY_LOGIN);
        assertNotNull(simpleUser);
        assertEquals(MY_LOGIN, simpleUser.getLoginName());
        assertEquals(FIRST_NAME, simpleUser.getFirstName());
        assertEquals(SURNAME, simpleUser.getSurname());
        assertEquals(PASSWORD_HASH, simpleUser.getPasswordHash());

        simpleUser.setFirstName(FIRST_NAME_MODIFIED);
        simpleUser.setSurname(SURNAME_MODIFIED);
        simpleUser.setPasswordHash(PASSWORD_HASH_MODIFIED);
        userDao.update(simpleUser);

        final OpenLUser modifiedUser = userDao.getUserByName(MY_LOGIN);
        assertNotNull(modifiedUser);
        assertEquals(FIRST_NAME_MODIFIED, modifiedUser.getFirstName());
        assertEquals(SURNAME_MODIFIED, modifiedUser.getSurname());
        assertEquals(PASSWORD_HASH_MODIFIED, modifiedUser.getPasswordHash());

        userDao.deleteUserByName(MY_LOGIN);
        assertNull(userDao.getUserByName(MY_LOGIN));

    }
}
