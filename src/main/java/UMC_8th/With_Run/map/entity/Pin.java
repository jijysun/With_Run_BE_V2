package UMC_8th.With_Run.map.entity;

import UMC_8th.With_Run.course.entity.Course;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Pin {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String color;
    private String detail;
    private Double latitude;
    private Double longitude;
    private int pinOrder;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "course_id", nullable = true)
    private Course course;

    public Long getCourseId() {
        return course != null ? course.getId() : null;
    }

    public void setCourseId(Long courseId) {
        if (this.course == null) {
            this.course = new Course();
        }
        this.course.setId(courseId);
    }

    public void setCourse(Course course) {
        this.course = course;
    }
}
