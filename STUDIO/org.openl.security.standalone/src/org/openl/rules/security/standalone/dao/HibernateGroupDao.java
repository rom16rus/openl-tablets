package org.openl.rules.security.standalone.dao;

import java.util.List;

import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

import org.openl.rules.security.standalone.persistence.OpenLGroup;
import org.springframework.transaction.annotation.Transactional;

/**
 * Hibernate implementation of {@link GroupDao}.
 *
 * @author Andrey Naumenko
 */
public class HibernateGroupDao extends BaseHibernateDao<OpenLGroup> implements GroupDao {

    @Override
    @Transactional(readOnly = true)
    public OpenLGroup getGroupByName(final String name) {
        CriteriaBuilder builder = getSession().getCriteriaBuilder();
        CriteriaQuery<OpenLGroup> criteria = builder.createQuery(OpenLGroup.class);
        Root<OpenLGroup> g = criteria.from(OpenLGroup.class);
        criteria.select(g).where(builder.equal(g.get("name"), name)).distinct(true);
        List<OpenLGroup> groupList = getSession().createQuery(criteria).getResultList();
        return groupList.stream().findFirst().orElse(null);
    }

    @Override
    @Transactional
    public void deleteGroupByName(final String name) {
        getSession().createNativeQuery(
            "delete from OpenL_Group2Group where includedGroupID = (select id from OpenL_Groups where groupName = :name)")
            .setParameter("name", name)
            .executeUpdate();
        getSession().createNativeQuery("delete from OpenL_Groups where groupName = :name")
            .setParameter("name", name)
            .executeUpdate();
    }

    @Override
    @Transactional(readOnly = true)
    public List<OpenLGroup> getAllGroups() {
        CriteriaBuilder builder = getSession().getCriteriaBuilder();
        CriteriaQuery<OpenLGroup> criteria = builder.createQuery(OpenLGroup.class);
        Root<OpenLGroup> root = criteria.from(OpenLGroup.class);
        criteria.select(root).orderBy(builder.asc(builder.upper(root.get("name"))));
        return getSession().createQuery(criteria).getResultList();
    }
}
