package org.openl.rules.security.standalone.dao;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Root;

import org.hibernate.Session;
import org.openl.rules.security.standalone.persistence.OpenLAccessEntry;
import org.openl.rules.security.standalone.persistence.OpenLUser;
import org.openl.rules.security.standalone.persistence.OpenLUserAccessEntry;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hibernate implementation of {@link UserDao}.
 *
 * @author Andrey Naumenko
 * @author Andrei Astrouski
 */
public class HibernateUserDao extends BaseHibernateDao<OpenLUser> implements UserDao {

    @Override
    @Transactional
    public OpenLUser getUserByName(final String name) {
        CriteriaBuilder builder = getSession().getCriteriaBuilder();
        CriteriaQuery<OpenLUser> criteria = builder.createQuery(OpenLUser.class);
        Root<OpenLUser> u = criteria.from(OpenLUser.class);
        u.fetch("accessEntries", JoinType.LEFT);
        criteria.select(u).where(builder.equal(u.get("loginName"), name)).distinct(true);
        List<OpenLUser> results = getSession().createQuery(criteria).getResultList();
        return results.stream().findFirst().orElse(null);
    }

    @Override
    @Transactional
    public void deleteUserByName(final String name) {
        getSession().createNativeQuery("delete from OpenL_Users where loginName = :name")
            .setParameter("name", name)
            .executeUpdate();
    }

    @Override
    @Transactional
    public List<OpenLUser> getAllUsers() {
        CriteriaBuilder builder = getSession().getCriteriaBuilder();
        CriteriaQuery<OpenLUser> criteria = builder.createQuery(OpenLUser.class);
        Root<OpenLUser> root = criteria.from(OpenLUser.class);
        root.fetch("accessEntries", JoinType.LEFT);
        criteria.select(root).orderBy(builder.asc(builder.upper(root.get("loginName"))));
        return getSession().createQuery(criteria).getResultList();
    }

    @Override
    public List<OpenLUserAccessEntry> getUserAccessRights(String loginName) {
        final Session session = getSession();
        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery<OpenLUserAccessEntry> userCriteriaQuery = builder.createQuery(OpenLUserAccessEntry.class);

        Root<OpenLUserAccessEntry> userEntry = userCriteriaQuery.from(OpenLUserAccessEntry.class);
        userEntry.fetch("user", JoinType.INNER);
        userEntry.fetch("openLSecurityObject", JoinType.INNER);
        userCriteriaQuery.select(userEntry).where(builder.equal(userEntry.get("loginname"), loginName));
        return session.createQuery(userCriteriaQuery).getResultList();
    }

    public List<OpenLAccessEntry> getUserAccessEntries(String loginName) {
        return new ArrayList<>();
    }
}
