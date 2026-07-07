package UMC_8th.With_Run.map.dto;

import lombok.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public class MapRequestDTO {

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class PinRequestDto {
        private String name;
        private String detail;
        private String color;
        private Double latitude;
        private Double longitude;
        private Integer pinOrder;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class RegionRequestDto {
        private Long id;
        private String name;
    }

    @Getter @Setter @NoArgsConstructor @AllArgsConstructor
    public static class CourseCreateRequestDto {
        private String name;
        private String description;
        private Integer time;
        private List<String> keywords; // 키워드 배열 (JSON 필드명과 일치)
        private List<PinRequestDto> pins;
        // userId는 JWT 토큰에서 자동 추출하므로 제거
        private Long regionProvinceId;
        private Long regionsCityId;
        private Long regionsTownId;
        private String overviewPolyline;
    }

    @Getter @Setter
    public static class CourseCreateMultiPartRequestDto {
        private MapRequestDTO.CourseCreateRequestDto courseCreateRequest;

        private MultipartFile courseImg;
    }
}
