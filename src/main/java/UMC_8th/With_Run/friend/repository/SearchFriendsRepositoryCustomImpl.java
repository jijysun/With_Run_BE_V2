package UMC_8th.With_Run.friend.repository;

import UMC_8th.With_Run.user.entity.Profile;
import UMC_8th.With_Run.user.entity.User;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.criteria.*;

import java.util.ArrayList;
import java.util.List;

public class SearchFriendsRepositoryCustomImpl implements SearchFriendsRepositoryCustom {
    @PersistenceContext
    private EntityManager em;

    @Override
    public List<User> searchFriendsByKeyword(Long provinceId, Long cityId, Long townId, Long excludeUserId, String keyword) {
        CriteriaBuilder cb = em.getCriteriaBuilder();
        CriteriaQuery<User> cq = cb.createQuery(User.class);
        Root<User> user = cq.from(User.class);
        Join<User, Profile> profile = user.join("profile", JoinType.INNER);

        List<Predicate> predicates = new ArrayList<>();

        // 지역 조건
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

        // keyword 조건
        if (keyword != null && !keyword.isBlank()) {
            String likeKeyword = "%" + keyword + "%";

            Predicate nameLike = cb.like(profile.get("name"), likeKeyword);
            Predicate genderLike = cb.like(profile.get("gender"), likeKeyword);
            Predicate birthLike = cb.like(profile.get("birth"), likeKeyword);  // 변경된 부분
            Predicate breedLike = cb.like(profile.get("breed"), likeKeyword);
            Predicate sizeLike = cb.like(profile.get("size"), likeKeyword);
            Predicate styleLike = cb.like(profile.get("style"), likeKeyword);
            Predicate characterLike = cb.like(profile.get("characters"), likeKeyword);

            Predicate keywordPredicate = cb.or(nameLike, genderLike, birthLike, breedLike, sizeLike, styleLike, characterLike);
            predicates.add(keywordPredicate);
        }

        cq.where(cb.and(predicates.toArray(new Predicate[0]))).distinct(true);

        return em.createQuery(cq).getResultList();
    }
}
