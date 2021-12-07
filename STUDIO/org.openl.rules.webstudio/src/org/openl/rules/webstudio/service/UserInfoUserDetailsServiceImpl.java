package org.openl.rules.webstudio.service;

import java.util.Collections;
import java.util.function.Function;

import org.openl.rules.security.SimpleUser;
import org.openl.rules.security.UserExternalFlags;
import org.openl.rules.security.UserExternalFlags.Feature;
import org.openl.rules.security.standalone.dao.UserDao;
import org.openl.rules.security.standalone.persistence.User;
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
    public org.openl.rules.security.User loadUserByUsername(String name) throws UsernameNotFoundException,
                                                                         DataAccessException {
        adminUsersInitializer.initIfSuperuser(name);
        User user = userDao.getUserByName(name);
        if (user == null) {
            throw new UsernameNotFoundException(String.format("Unknown user: '%s'", name));
        }

        UserExternalFlags externalFlags = UserExternalFlags.builder()
            .applyFeature(Feature.EXTERNAL_FIRST_NAME, user.isFirstNameExternal())
            .applyFeature(Feature.EXTERNAL_LAST_NAME, user.isLastNameExternal())
            .applyFeature(Feature.EXTERNAL_EMAIL, user.isEmailExternal())
            .applyFeature(Feature.EXTERNAL_DISPLAY_NAME, user.isDisplayNameExternal())
            .build();

        SimpleUser simpleUser = new SimpleUser(user.getFirstName(),
            user.getSurname(),
            user.getLoginName(),
            user.getPasswordHash(),
            Collections.emptySet(),
            user.getEmail(),
            user.getDisplayName(),
            externalFlags);
        return authoritiesMapper.apply(simpleUser);
    }
}
