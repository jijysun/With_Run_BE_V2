package UMC_8th.With_Run.domain.notice.controller;

import UMC_8th.With_Run.global.apiResponse.status.ErrorCode;
import UMC_8th.With_Run.global.exception.GeneralException;
import UMC_8th.With_Run.global.security.jwt.JwtTokenProvider;
import UMC_8th.With_Run.domain.notice.dto.NoticeResponse;
import UMC_8th.With_Run.domain.notice.service.NoticeService;
import UMC_8th.With_Run.domain.user.entity.User;
import UMC_8th.With_Run.domain.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notice")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<?> getNoticeList(HttpServletRequest request) {

        Authentication authentication = jwtTokenProvider.extractAuthentication(request);
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorCode.WRONG_USER));
        Long userId = user.getId();

        List<NoticeResponse> notices = noticeService.getUserNotices(userId);
        return ResponseEntity.ok(notices);
    }
}
