package UMC_8th.With_Run.user.repository;

import UMC_8th.With_Run.course.entity.Course;
import UMC_8th.With_Run.user.entity.Scraps;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import UMC_8th.With_Run.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ScrapsRepository extends JpaRepository<Scraps, Long> {
    List<Scraps> findAllByUserId(Long userId);

    @Query("SELECT s FROM Scraps s JOIN FETCH s.course WHERE s.user.id = :userId")
    List<Scraps> findAllByUserIdWithCourse(@Param("userId") Long userId);

    @Query("SELECT s.course.id, COUNT(s) FROM Scraps s WHERE s.course.id IN :courseIds AND s.deletedAt IS NULL GROUP BY s.course.id")
    List<Object[]> getScrapCountsByCourseIds(@Param("courseIds") List<Long> courseIds);

    boolean existsByUserAndCourse(User user, Course course);
    Optional<Scraps> findByUserAndCourse(User user, Course course);


}
