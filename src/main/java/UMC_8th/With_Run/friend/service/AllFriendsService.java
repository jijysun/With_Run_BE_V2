package UMC_8th.With_Run.friend.service;

import UMC_8th.With_Run.friend.dto.FriendsResponse;
import UMC_8th.With_Run.friend.repository.FriendsRepository;
import UMC_8th.With_Run.user.entity.Profile;
import UMC_8th.With_Run.user.entity.User;
import UMC_8th.With_Run.user.repository.UserRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AllFriendsService {
    private final FriendsRepository friendsRepository;
    private final UserRepository userRepository; // 기준 사용자 프로필 가져올 때 필요
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<FriendsResponse> findUsersByRegion(Long provinceId, Long cityId, Long townId, Long userId) {
        // 기준 사용자 프로필
        User me = userRepository.findById(userId).orElseThrow(() -> new RuntimeException("User not found"));
        Profile myProfile = me.getProfile();

        List<String> myStyles = myProfile != null && myProfile.getStyle() != null
                ? parseJsonArray(myProfile.getStyle())
                : List.of();

        List<String> myCharacters = myProfile != null && myProfile.getCharacters() != null
                ? parseJsonArray(myProfile.getCharacters())
                : List.of();

        List<User> users = friendsRepository.findUsersByRegion(provinceId, cityId, townId, userId);

        return users.stream()
                .map(user -> {
                    Profile profile = user.getProfile();

                    List<String> styleList = profile != null && profile.getStyle() != null
                            ? parseJsonArray(profile.getStyle())
                            : List.of();

                    List<String> charactersList = profile != null && profile.getCharacters() != null
                            ? parseJsonArray(profile.getCharacters())
                            : List.of();

                    // 공통 성향 구하기
                    List<String> common = getCommonTraits(myCharacters, myStyles, charactersList, styleList);

                    return new FriendsResponse(
                            user.getId(),
                            profile != null ? profile.getName() : null,
                            profile != null ? profile.getProfileImage() : null,
                            styleList,
                            charactersList,
                            common
                    );
                })
                .collect(Collectors.toList());
    }

    private List<String> getCommonTraits(List<String> myChars, List<String> myStyles,
                                         List<String> otherChars, List<String> otherStyles) {
        List<String> common = new ArrayList<>();

        // characters 중 1개 공통 찾기
        for (String c : myChars) {
            if (otherChars.contains(c)) {
                common.add(c);
                break;
            }
        }

        // styles 중 1개 공통 찾기
        for (String s : myStyles) {
            if (otherStyles.contains(s)) {
                common.add(s);
                break;
            }
        }

        return common;
    }

    private List<String> parseJsonArray(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of(); // 파싱 실패 시 빈 리스트 반환
        }
    }

}
