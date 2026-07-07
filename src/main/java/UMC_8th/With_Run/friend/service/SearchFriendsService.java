package UMC_8th.With_Run.friend.service;

import UMC_8th.With_Run.friend.dto.FriendsResponse;
import UMC_8th.With_Run.friend.repository.SearchFriendsRepository;
import UMC_8th.With_Run.user.entity.Profile;
import UMC_8th.With_Run.user.entity.User;
import UMC_8th.With_Run.user.repository.ProfileRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchFriendsService {

    private final SearchFriendsRepository searchFriendsRepository;
    private final ProfileRepository profileRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<FriendsResponse> searchFriends(Long provinceId, Long cityId, Long townId, Long userId, String keyword) {
        Profile myProfile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("내 프로필이 없습니다."));

        List<User> matchedUsers = searchFriendsRepository.searchFriendsByKeyword(provinceId, cityId, townId, userId, keyword);

        // 문자열 → 리스트로 변환
        List<String> myCharacters = parseJsonArray(myProfile.getCharacters());
        List<String> myStyles = parseJsonArray(myProfile.getStyle());

        return matchedUsers.stream().map(user -> {
            Profile profile = profileRepository.findByUserId(user.getId())
                    .orElseThrow(() -> new IllegalArgumentException("상대 프로필이 없습니다."));

            LocalDate birthDate = LocalDate.parse(profile.getBirth());
            int age = LocalDate.now().getYear() - birthDate.getYear();

            List<String> targetCharacters = parseJsonArray(profile.getCharacters());
            List<String> targetStyles = parseJsonArray(profile.getStyle());

            // 공통 요소 계산
            String common = myCharacters.stream()
                    .filter(targetCharacters::contains)
                    .findFirst()
                    .orElse(null);

            if (common == null) {
                common = myStyles.stream()
                        .filter(targetStyles::contains)
                        .findFirst()
                        .orElse(null);
            }

            return FriendsResponse.builder()
                    .userId(user.getId())
                    .userName(profile.getName())
                    .profileImage(profile.getProfileImage())
                    .characters(targetCharacters)
                    .style(targetStyles)
                    .common(Collections.singletonList(common))
                    .build();
        }).collect(Collectors.toList());
    }

    private List<String> parseJsonArray(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return List.of(); // 파싱 실패 시 빈 리스트 반환
        }
    }
}
