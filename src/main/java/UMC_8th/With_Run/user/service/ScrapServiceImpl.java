package UMC_8th.With_Run.user.service;

import UMC_8th.With_Run.common.apiResponse.status.ErrorCode;
import UMC_8th.With_Run.common.exception.handler.UserHandler;
import UMC_8th.With_Run.common.security.jwt.JwtTokenProvider;
import UMC_8th.With_Run.course.entity.Course;
import UMC_8th.With_Run.user.dto.UserResponseDto.ScrapItemDTO;
import UMC_8th.With_Run.user.dto.UserResponseDto.ScrapListResultDTO;
import UMC_8th.With_Run.user.entity.Scraps;
import UMC_8th.With_Run.user.entity.User;
import UMC_8th.With_Run.user.repository.ScrapsRepository;
import UMC_8th.With_Run.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ScrapServiceImpl implements ScrapService {

    private final ScrapsRepository scrapsRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public ScrapListResultDTO getScrapsByCurrentUser(HttpServletRequest request) {
        Authentication authentication = jwtTokenProvider.extractAuthentication(request);
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserHandler(ErrorCode.WRONG_USER));

        List<Scraps> scraps = scrapsRepository.findAllByUserIdWithCourse(user.getId());

        List<ScrapItemDTO> scrapItems = scraps.stream()
                .map(scrap -> {
                    Course course = scrap.getCourse();

                    return ScrapItemDTO.builder()
                            .courseId(course.getId())
                            .courseName(course.getName())
                            .keyword(course.getKeyWord())
                            .time(course.getTime())
                            .courseImage(course.getCourseImage())
                            .location(course.getLocation())
                            .scrapedAt(scrap.getCreatedAt())
                            .build();
                })
                .toList();

        return ScrapListResultDTO.builder()
                .scrapList(scrapItems)
                .build();
    }
}
