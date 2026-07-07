package UMC_8th.With_Run.friend.repository;

import UMC_8th.With_Run.user.entity.Profile;
import UMC_8th.With_Run.user.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

@Repository
public class FriendsRepositoryCustomImpl implements FriendsRepositoryCustom {

    @PersistenceContext
    private EntityManager em;

    @Override
    public List<User> findUsersByRegion(Long provinceId, Long cityId, Long townId, Long excludeUserId) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<User> cq = cb.createQuery(User.class);
        Root<User> user = cq.from(User.class);
        Join<User, Profile> profile = user.join("profile", JoinType.INNER);

        List<Predicate> predicates = new ArrayList<>();

        if (excludeUserId != null) {
            predicates.add(cb.notEqual(user.get("id"), excludeUserId));
        }
        if (provinceId != null) {
            predicates.add(cb.equal(profile.get("provinceId"), provinceId));
        }
        if (cityId != null) {
            predicates.add(cb.equal(profile.get("cityId"), cityId));
        }
        if (townId != null) {
            predicates.add(cb.equal(profile.get("townId"), townId));
        }
        predicates.add(cb.isNull(profile.get("deletedAt")));

        cq.where(cb.and(predicates.toArray(new Predicate[0])));

        return em.createQuery(cq).getResultList();
    }

}
