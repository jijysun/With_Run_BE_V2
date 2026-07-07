package UMC_8th.With_Run.course.repository;

import UMC_8th.With_Run.course.entity.Course;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {

    @Query(value = """
    SELECT c.*
       FROM course c
       LEFT JOIN likes l ON l.course_id = c.id AND l.deleted_at IS NULL
       WHERE c.region_province_id = :provinceId
        AND (:cityId IS NULL OR c.regions_city_id = :cityId)
        AND (:townId IS NULL OR c.regions_town_id = :townId)
         AND c.deleted_at IS NULL
       GROUP BY c.id
       ORDER BY COUNT(l.id) DESC;
           
""", nativeQuery = true)
    List<Course> findCoursesByRegion(Long provinceId, Long cityId, Long townId);

    List<Course> findAllByUserId(Long userId);
}
