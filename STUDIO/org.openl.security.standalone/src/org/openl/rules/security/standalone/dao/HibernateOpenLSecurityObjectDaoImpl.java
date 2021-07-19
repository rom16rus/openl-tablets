package org.openl.rules.security.standalone.dao;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.JoinType;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import org.hibernate.Session;
import org.openl.rules.security.standalone.persistence.OpenLAccessEntry;
import org.openl.rules.security.standalone.persistence.OpenLGroupAccessEntry;
import org.openl.rules.security.standalone.persistence.OpenLSecurityObject;
import org.openl.rules.security.standalone.persistence.OpenLUserAccessEntry;
import org.openl.rules.security.standalone.persistence.SecurityObjectType;
import org.springframework.transaction.annotation.Transactional;

public class HibernateOpenLSecurityObjectDaoImpl extends BaseHibernateDao<OpenLSecurityObject> implements OpenLSecurityObjectDao {

    @Override
    @Transactional(readOnly = true)
    public List<OpenLSecurityObject> getByType(SecurityObjectType objectType) {
        final Session session = getSession();
        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery<OpenLSecurityObject> criteria = builder.createQuery(OpenLSecurityObject.class);
        Root<OpenLSecurityObject> securityObjectRoot = criteria.from(OpenLSecurityObject.class);
        criteria.select(securityObjectRoot).where(builder.equal(securityObjectRoot.get("type"), objectType));
        return session.createQuery(criteria).getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OpenLSecurityObject> findAll() {
        final Session session = getSession();
        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery<OpenLSecurityObject> criteria = builder.createQuery(OpenLSecurityObject.class);
        Root<OpenLSecurityObject> securityObjectRoot = criteria.from(OpenLSecurityObject.class);
        criteria.select(securityObjectRoot);
        return session.createQuery(criteria).getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OpenLSecurityObject> findByNameAndType(String name, SecurityObjectType objectType) {
        final Session session = getSession();
        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery<OpenLSecurityObject> criteria = builder.createQuery(OpenLSecurityObject.class);
        Root<OpenLSecurityObject> securityObjectRoot = criteria.from(OpenLSecurityObject.class);
        List<Predicate> predicates = new ArrayList<>();
        Predicate objectTypePredicate = builder.and(builder.equal(securityObjectRoot.get("type"), objectType));
        Predicate namePredicate = builder.and(builder.equal(securityObjectRoot.get("name"), name));
        predicates.add(objectTypePredicate);
        predicates.add(namePredicate);
        criteria.select(securityObjectRoot).where(predicates.toArray(new Predicate[0]));
        return session.createQuery(criteria).getResultList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OpenLAccessEntry> getObjectAccessRights(SecurityObjectType type, String name) {
        final Session session = getSession();

        CriteriaBuilder builder = session.getCriteriaBuilder();
        CriteriaQuery<OpenLUserAccessEntry> userCriteriaQuery = builder.createQuery(OpenLUserAccessEntry.class);
        Root<OpenLUserAccessEntry> userEntry = userCriteriaQuery.from(OpenLUserAccessEntry.class);
        userEntry.fetch("user", JoinType.INNER);
        userEntry.fetch("openLSecurityObject", JoinType.INNER);

        List<Predicate> predicates = new ArrayList<>();
        Predicate objectTypePredicate = builder
            .and(builder.equal(userEntry.get("openLSecurityObject").get("type"), type));
        Predicate namePredicate = builder.and(builder.equal(userEntry.get("openLSecurityObject").get("name"), name));
        predicates.add(objectTypePredicate);
        predicates.add(namePredicate);

        userCriteriaQuery.select(userEntry).where(predicates.toArray(new Predicate[0]));

        List<OpenLUserAccessEntry> userEntries = session.createQuery(userCriteriaQuery).getResultList();

        List<OpenLAccessEntry> result = new ArrayList<>(userEntries);

        CriteriaQuery<OpenLGroupAccessEntry> groupCriteriaQuery = builder.createQuery(OpenLGroupAccessEntry.class);
        Root<OpenLGroupAccessEntry> groupEntry = groupCriteriaQuery.from(OpenLGroupAccessEntry.class);
        groupEntry.fetch("openLSecurityObject", JoinType.INNER);
        groupEntry.fetch("group", JoinType.INNER);

        groupCriteriaQuery.select(groupEntry).where(predicates.toArray(new Predicate[0]));

        List<OpenLGroupAccessEntry> groupEntries = session.createQuery(groupCriteriaQuery).getResultList();
        result.addAll(groupEntries);
        return result;
    }
}
