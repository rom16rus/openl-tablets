package org.openl.rules.security.standalone.dao;

import java.util.List;

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
        return (OpenLGroup) getSession().createQuery(
            "select distinct og from OpenLGroup og "
                    + " left join fetch og.accessEntries ae "
                    + " left join fetch ae.openLSecurityObject "
                    + " left join fetch og.includedGroupLinks g2g "
                    + " left join fetch og.parentGroupLinks p2g "
                    + " left join fetch p2g.parentGroup pg "
                    + " left join fetch pg.accessEntries pgae"
                    + " left join fetch pgae.openLSecurityObject "
                    + " left join fetch g2g.includedGroup cg "
                    + " left join fetch cg.accessEntries cgae "
                    + " left join fetch cgae.openLSecurityObject "
                    + " where (og.name = :name)")
            .setParameter("name", name)
            .uniqueResult();
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
    @SuppressWarnings("unchecked")
    public List<OpenLGroup> getAllGroups() {
        return (List<OpenLGroup>) getSession().createQuery(
            "select distinct og from OpenLGroup og "
                    + " left join fetch og.accessEntries ae "
                    + " left join fetch og.includedGroupLinks g2g " +
                    " left join fetch og.parentGroupLinks p2g " +
                    " left join fetch p2g.parentGroup pg "
                    + " left join fetch pg.accessEntries pgae"
                    + " left join fetch g2g.includedGroup cg "
                    + " left join fetch cg.accessEntries cgae"
                    + " order by og.name")
            .setReadOnly(true)
            .getResultList();
    }
}
