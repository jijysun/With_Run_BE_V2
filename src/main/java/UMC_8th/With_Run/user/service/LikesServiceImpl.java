package UMC_8th.With_Run.user.service;

import UMC_8th.With_Run.common.apiResponse.status.ErrorCode;
import UMC_8th.With_Run.common.exception.handler.UserHandler;
import UMC_8th.With_Run.common.security.jwt.JwtTokenProvider;
import UMC_8th.With_Run.course.entity.Course;
import UMC_8th.With_Run.user.dto.UserResponseDto.LikeItemDTO;
import UMC_8th.With_Run.user.dto.UserResponseDto.LikeListResultDTO;
import UMC_8th.With_Run.user.entity.Likes;
import UMC_8th.With_Run.user.entity.User;
import UMC_8th.With_Run.user.repository.LikesRepository;
import UMC_8th.With_Run.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class LikesServiceImpl implements LikesService {

    private final LikesRepository likesRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public LikeListResultDTO getLikesByCurrentUser(HttpServletRequest request) {
        Authentication authentication = jwtTokenProvider.extractAuthentication(request);
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserHandler(ErrorCode.WRONG_USER));

        List<Likes> likes = likesRepository.findAllByUserIdWithCourse(user.getId());

        List<LikeItemDTO> likeItems = likes.stream()
                .map(like -> {
                    Course course = like.getCourse();

                    return LikeItemDTO.builder()
                            .courseId(course.getId())
                            .courseName(course.getName())
                            .keyword(course.getKeyWord())
                            .time(course.getTime())
                            .courseImage(course.getCourseImage())
                            .location(course.getLocation())
                            .likedAt(like.getCreatedAt())
                            .build();
                })
                .toList();

        return LikeListResultDTO.builder()
                .likeList(likeItems)
                .build();
    }
}

