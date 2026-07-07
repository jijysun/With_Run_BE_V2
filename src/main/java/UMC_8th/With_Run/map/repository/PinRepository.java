package UMC_8th.With_Run.map.repository;

import UMC_8th.With_Run.course.entity.Course;
import UMC_8th.With_Run.map.entity.Pin;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PinRepository extends JpaRepository<Pin, Long> {
    List<Pin> findByCourseOrderByPinOrderAsc(Course course);
    List<Pin> findAllByCourse(Course course);
}
