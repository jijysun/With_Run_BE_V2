package UMC_8th.With_Run.friend.service;

import java.time.LocalDate;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import UMC_8th.With_Run.friend.dto.FriendDetailResponse;
import UMC_8th.With_Run.friend.repository.FriendDetailRepository;
import UMC_8th.With_Run.user.entity.Profile;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FriendDetailService {

    private final FriendDetailRepository friendDetailRepository;

    public FriendDetailResponse getFriendDetail(Long userId) {
        Profile profile = friendDetailRepository.findByUserIdAndDeletedAtIsNull(userId)
                .orElseThrow(() -> new RuntimeException("해당 유저의 프로필이 존재하지 않습니다."));

        return new FriendDetailResponse(
                userId,
                profile.getName(),
                profile.getProfileImage(),
                profile.getBreed(),
                profile.getGender(),
                calculateAge(profile.getBirth()),
                profile.getSize(),
                profile.getIntroduction()
        );
    }

    private String calculateAge(String birthString) {
        if (birthString == null || birthString.isEmpty()) return "정보 없음";

        try {
            LocalDate birth = LocalDate.parse(birthString, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            LocalDate now = LocalDate.now();
            Period period = Period.between(birth, now);

            int years = period.getYears();
            int months = period.getMonths();

            if (years == 0 && months == 0) {
                return "0개월";
            } else if (years == 0) {
                return months + "개월";
            } else if (months == 0) {
                return years + "년";
            } else {
                return years + "년 " + months + "개월";
            }

        } catch (Exception e) {
            return "형식 오류";
        }
    }


}