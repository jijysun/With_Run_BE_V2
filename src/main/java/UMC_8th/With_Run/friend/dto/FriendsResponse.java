package UMC_8th.With_Run.friend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Builder
@Data
@AllArgsConstructor
public class FriendsResponse {
    private Long userId;
    private String userName;
    private String profileImage;
    private List<String> style;
    private List<String> characters;
    private List<String> common;
}
