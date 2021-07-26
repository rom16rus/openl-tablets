package org.openl.rules.security.standalone.dao;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Fetch;
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
        return (OpenLUser) getSession().createQuery(
            "select distinct u from OpenLUser u" +
                    " left join fetch u.accessEntries uae " +
                    " left join fetch uae.openLSecurityObject " +
                    " left join fetch u.groups ug " +
                    " left join fetch ug.accessEntries ugae" +
                    " left join fetch ugae.openLSecurityObject " +
                    " left join fetch ug.includedGroupLinks ggl " +
                    " left join fetch ug.parentGroupLinks pgl "+
                    " left join fetch pgl.parentGroup pg " +
                    " left join fetch pg.accessEntries pgae" +
                    " left join fetch pgae.openLSecurityObject" +
                    " left join fetch ggl.includedGroup ig "  +
                    " left join fetch ig.accessEntries igae " +
                    " left join fetch igae.openLSecurityObject "
                    + " where (u.loginName = :name)")
            .setParameter("name", name)
            .uniqueResult();
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
    @SuppressWarnings("unchecked")
    public List<OpenLUser> getAllUsers() {
        return (List<OpenLUser>) getSession().createQuery(
            "select distinct u from OpenLUser u " +
                    " left join fetch u.accessEntries ae " +
                    " left join fetch ae.openLSecurityObject" +
                    " left join fetch u.groups ug " +
                    " left join fetch ug.accessEntries ugae"
                    +" left join fetch ugae.openLSecurityObject "
                    + " left join fetch ug.includedGroupLinks g2g " +
                    " left join fetch ug.parentGroupLinks p2g " +
                    " left join fetch g2g.includedGroup ig "
                    + " left join fetch ig.accessEntries igae "
                    + " left join fetch igae.openLSecurityObject "
                    + " left join fetch p2g.parentGroup pg "+
                    " left join fetch pg.accessEntries pgae"
                    + " left join fetch pgae.openLSecurityObject"
                    + " order by u.loginName")
            .getResultList();
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

}
