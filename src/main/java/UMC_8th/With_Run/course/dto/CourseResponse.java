package UMC_8th.With_Run.course.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CourseResponse {
    private Long courseId;
    private String name;
    private List<String> keyword;
    private Integer time;
    private String courseImage;
    private String location;
}
