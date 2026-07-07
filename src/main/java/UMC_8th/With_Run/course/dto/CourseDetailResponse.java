package UMC_8th.With_Run.course.dto;

import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
public class CourseDetailResponse {
    private Long id;
    private String name;
    private String imageUrl;
    private List<String> keywords;
    private String description;
    private Integer time;
    private List<PinResponse> pins;
    
    // 추가된 필드들
    private String overviewPolyline; // 코스 전체 경로 Polyline 문자열
    private Boolean isLiked; // 사용자가 해당 코스를 좋아요 했는지 여부
    private Boolean isScrapped; // 사용자가 해당 코스를 스크랩 했는지 여부

    @Getter
    @Builder
    public static class PinResponse {
        private Long id;
        private String name;
        private String color;
        private double latitude;
        private double longitude;
        private String detail;
    }
}
