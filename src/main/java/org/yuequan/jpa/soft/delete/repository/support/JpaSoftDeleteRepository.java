package org.yuequan.jpa.soft.delete.repository.support;

import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.domain.Specifications;
import org.springframework.data.jpa.provider.PersistenceProvider;
import org.springframework.data.jpa.repository.query.QueryUtils;
import org.springframework.data.jpa.repository.support.CrudMethodMetadata;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.JpaEntityInformationSupport;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;
import org.yuequan.jpa.soft.delete.repository.SoftDelete;

import javax.persistence.EntityManager;
import javax.persistence.LockModeType;
import javax.persistence.Query;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.*;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.*;

import static org.springframework.data.jpa.repository.query.QueryUtils.DELETE_ALL_QUERY_STRING;
import static org.springframework.data.jpa.repository.query.QueryUtils.applyAndBind;
import static org.springframework.data.jpa.repository.query.QueryUtils.getQueryString;


/**
 * Soft Delete override of the {@link SimpleJpaRepository} class.
 * @author yuequan
 * @param <T> the type of the entity to handle
 * @param <ID> the type of the entity's identifier
 * @see org.springframework.data.jpa.repository.support.SimpleJpaRepository
 */
@SoftDelete
public class JpaSoftDeleteRepository<T,ID extends Serializable> extends SimpleJpaRepository<T,ID> {

    private static final String ID_MUST_NOT_BE_NULL = "The given id must not be null!";
    private static final String SOFT_DELETE_FLAG_COLUMN = "removed_at";
    private static final String SOFT_DELETE_FLAG_PROPERTIES = "removedAt";

    private final JpaEntityInformation<T, ?> entityInformation;
    private final EntityManager em;
    private final PersistenceProvider provider;

    private @Nullable
    CrudMethodMetadata metadata;
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
    public void setRepositoryMethodMetadata(CrudMethodMetadata crudMethodMetadata) {
        super.setRepositoryMethodMetadata(crudMethodMetadata);
        this.metadata = crudMethodMetadata;
    }

    private String getDeleteAllQueryString(){
        StringBuilder softDeleteAllQueryBuilder = new StringBuilder();
        softDeleteAllQueryBuilder.append("UPDATE %s x ");
        softDeleteAllQueryBuilder.append("SET ");
        softDeleteAllQueryBuilder.append(SOFT_DELETE_FLAG_PROPERTIES);
        softDeleteAllQueryBuilder.append("=");
        softDeleteAllQueryBuilder.append(" :");
        softDeleteAllQueryBuilder.append(SOFT_DELETE_FLAG_PROPERTIES);
        String softDeleteAllQueryString = softDeleteAllQueryBuilder.toString();
        return getQueryString(softDeleteAllQueryString, entityInformation.getEntityName());
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
    public void deleteInBatch(Iterable<T> entities) {
        Assert.notNull(entities, "The given Iterable of entities not be null!");

        if (!entities.iterator().hasNext()) {
            return;
        }
        StringBuilder whereBuilder = new StringBuilder(getDeleteAllQueryString());

        Iterator<T> iterator = entities.iterator();
        int i = 0;

        whereBuilder.append(" where x in (:entities)");

        Query query = em.createQuery(whereBuilder.toString());

       query.setParameter(SOFT_DELETE_FLAG_PROPERTIES, new Date());
       query.setParameter("entities", entities);
       query.executeUpdate();
    }

    @Override
    @Transactional
    public void deleteAllInBatch() {
        em.createQuery(getDeleteAllQueryString())
                .setParameter(SOFT_DELETE_FLAG_PROPERTIES, new Date())
                .executeUpdate();
    }

    @Override
    public Optional<T> findById(ID id) {
       return super.findOne(Specification.where(new ByIdSpecification<T,ID>(id,entityInformation)));
    }

    @Override
    protected <S extends T> TypedQuery<Long> getCountQuery(Specification<S> spec, Class<S> domainClass) {
        return super.getCountQuery(spec != null ? spec.and(new DeletedSpecification<>()) : new DeletedSpecification<>(), domainClass);
    }

    private static final class ByIdSpecification<T,ID extends Serializable> implements Specification<T>{
        private final ID id;
        private final JpaEntityInformation<T, ?> information;

        public ByIdSpecification(ID id, JpaEntityInformation<T, ?> information) {
            this.id = id;
            this.information = information;
        }

        @Override
        public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
            final List<Predicate> predicates = new ArrayList<>();
            if(information.hasCompositeId()){
                information.getIdAttributeNames().forEach(name ->
                        predicates.add(criteriaBuilder.equal(root.get(name), information.getCompositeIdAttributeValue(id, name))));
                return criteriaBuilder.and(predicates.toArray(new Predicate[predicates.size()]));
            }
            return criteriaBuilder.equal(root.get(information.getIdAttribute().getName()), id);
        }
    }

    private static final class DeletedSpecification<T> implements Specification<T>{
        private boolean isDeleted;

        public DeletedSpecification(boolean isDeleted) {
            this.isDeleted = isDeleted;
        }

        public DeletedSpecification() {
            this.isDeleted = false;
        }

        @Override
        public Predicate toPredicate(Root<T> root, CriteriaQuery<?> query, CriteriaBuilder criteriaBuilder) {
            if(isDeleted){
                return criteriaBuilder.isNotNull(root.get(SOFT_DELETE_FLAG_PROPERTIES));
            }else{
                return criteriaBuilder.isNull(root.get(SOFT_DELETE_FLAG_PROPERTIES));
            }
        }
    }

    @Override
    protected <S extends T> TypedQuery<S> getQuery(Specification<S> spec, Class<S> domainClass, Sort sort) {
        return super.getQuery(spec != null ? spec.and(new DeletedSpecification<>()) : new DeletedSpecification<>(), domainClass, sort);
    }
}
