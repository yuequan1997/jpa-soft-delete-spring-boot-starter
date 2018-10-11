package org.yuequan.jpa.soft.delete.repository.support;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.domain.Specifications;
import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.JpaEntityInformationSupport;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.yuequan.jpa.soft.delete.repository.SoftDelete;

import javax.persistence.EntityManager;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaUpdate;
import javax.persistence.criteria.Predicate;
import javax.persistence.criteria.Root;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;


/**
 * Soft Delete override of the {@link SimpleJpaRepository} class.
 * @author yuequan
 * @param <T> the type of the entity to handle
 * @param <ID> the type of the entity's identifier
 * @see org.springframework.data.jpa.repository.support.SimpleJpaRepository
 */
@SoftDelete
public class JpaSoftDeleteRepository<T,ID> extends SimpleJpaRepository<T,ID> {

    private static final String ID_MUST_NOT_BE_NULL = "The given id must not be null!";
    private static final String SOFT_DELETE_FLAG_COLUMN = "removed_at";
    private static final String SOFT_DELETE_FLAG_PROPERTIES = "removedAt";

    private final JpaEntityInformation<T, ?> entityInformation;
    private final EntityManager em;
    private final PersistenceProvider provider;

    /**
     * Creates a new {@link SimpleJpaRepository} to manage objects of the given {@link JpaEntityInformation}.
     *
     * @param entityInformation must not be {@literal null}.
     * @param entityManager must not be {@literal null}.
     */
    public JpaSoftDeleteRepository(JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityInformation = entityInformation;
        this.em = entityManager;
        this.provider = PersistenceProvider.fromEntityManager(entityManager);
    }
    /**
     * Creates a new {@link SimpleJpaRepository} to manage objects of the given domain type.
     *
     * @param domainClass must not be {@literal null}.
     * @param em must not be {@literal null}.
     */
    public JpaSoftDeleteRepository(Class<T> domainClass, EntityManager em) {
        this(JpaEntityInformationSupport.getEntityInformation(domainClass, em), em);
    }

    @Override
    @Transactional
    public void deleteById(ID id) {
        Assert.notNull(id, ID_MUST_NOT_BE_NULL);
        delete(findById(id).orElseThrow(() -> new EmptyResultDataAccessException(
                String.format("No %s entity with id %s exists!", entityInformation.getJavaType(), id), 1)));
    }

    /**
     *
     * @param entity
     */
    @Override
    @Transactional
    public void delete(T entity) {
        Assert.notNull(entity, "The given entity must not be null!");
        CriteriaBuilder criteriaBuilder = em.getCriteriaBuilder();
        CriteriaUpdate<T> updater = criteriaBuilder.createCriteriaUpdate(getDomainClass());
        Root<T> root = updater.from(getDomainClass());
        updater.set(SOFT_DELETE_FLAG_PROPERTIES, new Date());
        final List<Predicate> predicates = new ArrayList<>();
        if(entityInformation.hasCompositeId()){
            entityInformation.getIdAttributeNames().forEach(idName -> {
                predicates.add(criteriaBuilder.equal(root.get(idName),
                        entityInformation.getCompositeIdAttributeValue(entityInformation.getId(entity), idName)));
            });
            updater.where(criteriaBuilder.and(predicates.toArray(new Predicate[predicates.size()])));
        }else{
            updater.where(criteriaBuilder.equal(root.get(entityInformation.getIdAttribute().getName()), entityInformation.getId(entity)));
        }
        em.createQuery(updater).executeUpdate();
    }

    @Override
    @Transactional
    public void deleteAll(Iterable<? extends T> entities) {
        Assert.notNull(entities, "The given Iterable of entities not be null!");
        for (T entity : entities) {
            delete(entity);
        }
    }

    @Override
    @Transactional
    public void deleteInBatch(Iterable<T> entities) {
        super.deleteInBatch(entities);
    }

    @Override
    @Transactional
    public void deleteAll() {
        for (T element : findAll()) {
            delete(element);
        }
    }

    @Override
    @Transactional
    public void deleteAllInBatch() {
        super.deleteAllInBatch();
    }
}
