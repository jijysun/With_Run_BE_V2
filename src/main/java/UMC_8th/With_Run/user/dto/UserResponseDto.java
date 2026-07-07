package UMC_8th.With_Run.user.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Getter;
import lombok.NoArgsConstructor;

public class UserResponseDto {
    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginResultDTO {
        Long userId;
        String accessToken;
        boolean isNewUser;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProfileResultDTO {
        private Long id;
        private Long userId;
        private Long provinceId;
        private String provinceName;
        private Long cityId;
        private String cityName;
        private Long townId;
        private String townName;
        private String name;
        private String gender;
        private String birth;
        private String breed;
        private String size;
        private String profileImage;

        private String character;
        private String style;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScrapItemDTO {
        private Long courseId;
        private String courseName;
        private String keyword;
        private Integer time;
        private String courseImage;
        private String location;
        private LocalDateTime scrapedAt;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FollowItemDTO {
        private Long targetUserId;
        private String name;
        private String profileImage;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FollowerItemDTO {
        private Long userId;
        private String name;
        private String profileImage;
    }


    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ScrapListResultDTO {
        private List<ScrapItemDTO> scrapList;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LikeItemDTO {
        private Long courseId;
        private String courseName;
        private String keyword;
        private Integer time;
        private String courseImage;
        private String location;
        private LocalDateTime likedAt;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LikeListResultDTO {
        private List<LikeItemDTO> likeList;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MyCourseItemDTO {
        private Long courseId;
        private String courseName;
        private String keyword;
        private Integer time;
        private String courseImage;
        private String location;
        private LocalDateTime createdAt;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MyCourseListResultDTO {
        private List<MyCourseItemDTO> myCourseList;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FollowerListResultDTO {
        private int count;
        private List<FollowerItemDTO> followers;
    }

    @Builder
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FollowingListResultDTO {
        private int count;
        private List<FollowItemDTO> followings;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class SimpleUserResultDTO {
        private String message;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class RegionResponseDTO {
        private RegionDTO province;
        private RegionDTO city;
        private RegionDTO town;

        @Getter @Builder
        public static class RegionDTO {
            private Long id;
            private String name;
        }
    }
}
