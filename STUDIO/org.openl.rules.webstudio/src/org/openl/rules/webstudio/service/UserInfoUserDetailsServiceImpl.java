package org.openl.rules.webstudio.service;

import java.util.Collections;
import java.util.function.Function;

import org.openl.rules.security.SimpleUser;
import org.openl.rules.security.User;
import org.openl.rules.security.standalone.dao.UserDao;
import org.openl.rules.security.standalone.persistence.OpenLUser;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * {@link org.springframework.security.core.userdetails.UserDetailsService} that can load UserInfo as UserDetails from
 * database.
 *
 * @author Andrey Naumenko
 * @author adjusted to new security model.
 */
public class UserInfoUserDetailsServiceImpl implements UserDetailsService {

    private final UserDao userDao;
    private final AdminUsers adminUsersInitializer;
    private final Function<SimpleUser, SimpleUser> authoritiesMapper;

    public UserInfoUserDetailsServiceImpl(UserDao userDao,
            AdminUsers adminUsersInitializer,
            Function<SimpleUser, SimpleUser> authoritiesMapper) {
        this.userDao = userDao;
        this.adminUsersInitializer = adminUsersInitializer;
        this.authoritiesMapper = authoritiesMapper;
    }

    @Override
    public User loadUserByUsername(String name) throws UsernameNotFoundException, DataAccessException {
        adminUsersInitializer.initIfSuperuser(name);
        OpenLUser openLUser = userDao.getUserByName(name);
        if (openLUser == null) {
            throw new UsernameNotFoundException(String.format("Unknown user: '%s'", name));
        }

        SimpleUser simpleUser = new SimpleUser(openLUser.getFirstName(),
            openLUser.getSurname(),
            openLUser.getLoginName(),
            openLUser.getPasswordHash(),
            Collections.emptySet());
        return authoritiesMapper.apply(simpleUser);
    }
}
