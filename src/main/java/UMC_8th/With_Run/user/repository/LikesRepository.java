package UMC_8th.With_Run.user.repository;

import UMC_8th.With_Run.course.entity.Course;
import UMC_8th.With_Run.user.entity.Likes;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import UMC_8th.With_Run.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LikesRepository extends JpaRepository<Likes, Long> {

    List<Likes> findAllByUserId(Long userId);

    @Query("SELECT l FROM Likes l JOIN FETCH l.course WHERE l.user.id = :userId")
    List<Likes> findAllByUserIdWithCourse(@Param("userId") Long userId);

    @Query("SELECT l.course.id, COUNT(l) FROM Likes l WHERE l.course.id IN :courseIds AND l.deletedAt IS NULL GROUP BY l.course.id")
    List<Object[]> getLikeCountsByCourseIds(@Param("courseIds") List<Long> courseIds);

    boolean existsByUserAndCourse(User user, Course course);

    Optional<Likes> findByUserAndCourse(User user, Course course);
}


