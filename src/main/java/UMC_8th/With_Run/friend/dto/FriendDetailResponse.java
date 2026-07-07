package UMC_8th.With_Run.friend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FriendDetailResponse {
    private Long userId;
    private String name;
    private String profileImage;
    private String breed;
    private String gender;
    private String age;
    private String size;
    private String introduction;
}
