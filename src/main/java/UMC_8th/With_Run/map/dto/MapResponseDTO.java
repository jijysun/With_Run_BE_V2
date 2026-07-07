package UMC_8th.With_Run.map.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

public class MapResponseDTO {

    @Builder @Getter @NoArgsConstructor @AllArgsConstructor
    public static class PlaceResponseDto {
        private Long id; // 실제 DB ID가 아니라면 placeId 해시로도 가능
        private String name;
        private String address;
        private Double latitude;
        private Double longitude;
        private String imageUrl;
        private String currentOperatingStatus;
        private String openingHours;
        private Boolean parkingAvailable;
    }

    @Builder @Getter @NoArgsConstructor @AllArgsConstructor
    public static class PinResponseDto {
        private Long pinId;

    }

    @Builder @Getter @NoArgsConstructor @AllArgsConstructor
    public static class GetPinDto {
        private Long pinId;
        private Long courseId;
        private String name;
        private String detail;
        private String color;
        private Double latitude;
        private Double longitude;
    }

    @Builder @Getter @NoArgsConstructor @AllArgsConstructor
    public static class CourseCreateResponseDto {
        private Long courseId;
        private String overviewPolyline;
    }

    @Builder @Getter @NoArgsConstructor @AllArgsConstructor
    public static class PetFacilityResponseDto {
        private Long id;
        private String name;
        private String category;
        private Double longitude;
        private Double latitude;
        private String address;
        private String closed_day;
        private String running_time;
        private String has_parking;
    }

    @Builder @Getter @NoArgsConstructor @AllArgsConstructor
    public static class PetFacilityPageResponseDto {
        private List<PetFacilityResponseDto> content;
        private long totalElements;
        private int totalPages;
        private int size;
        private int number;
        private boolean first;
        private boolean last;
    }
}
