package UMC_8th.With_Run.friend.controller;

import UMC_8th.With_Run.common.apiResponse.status.ErrorCode;
import UMC_8th.With_Run.common.exception.GeneralException;
import UMC_8th.With_Run.common.security.jwt.JwtTokenProvider;
import UMC_8th.With_Run.friend.dto.FriendsResponse;
import UMC_8th.With_Run.friend.dto.FriendDetailResponse;
import UMC_8th.With_Run.friend.service.*;
import UMC_8th.With_Run.user.entity.User;
import UMC_8th.With_Run.user.repository.UserRepository;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "친구 API", description = "친구 추천, 상세 조회, 팔로우, 차단, 검색 등 친구 관련 기능 제공")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/friends")
public class FriendController {

    private final AllFriendsService allFriendsService;
    private final FriendDetailService friendDetailService;
    private final RecommendedFriendsService recommendedFriendsService;
    private final SearchFriendsService searchFriendsService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final FollowFriendService followFriendService;
    private final BlockFriendService blockFriendService;
    private final ReportService reportService;

    @Operation(summary = "추천 친구 조회", description = "사용자에게 맞는 추천 친구들의 간단한 프로필 정보를 조회합니다.")
    @GetMapping("/recommendation")
    public List<FriendsResponse> getRecommendedFriends(
            @RequestParam(required = true) Long provinceId,
            @RequestParam(required = false) Long cityId,
            @RequestParam(required = false) Long townId,
            HttpServletRequest request) {

        Authentication authentication = jwtTokenProvider.extractAuthentication(request);
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorCode.WRONG_USER));
        Long userId = user.getId();

        return recommendedFriendsService.recommendedFriends(provinceId, cityId, townId, userId);
    }

    @Operation(summary = "사용자 세부 정보 조회", description = "특정 친구의 상세 정보를 조회합니다.")
    @GetMapping("/detail")
    public FriendDetailResponse getUserDetail(@RequestParam Long userId) {
        return friendDetailService.getFriendDetail(userId);
    }

    @Operation(summary = "동네 친구 전체 목록 조회", description = "동네의 모든 친구 목록을 조회합니다.")
    @GetMapping("/all")
    public List<FriendsResponse> getFriendsByRegion(
            @RequestParam(value = "provinceId", required = true) Long provinceId,
            @RequestParam(value = "cityId", required = false) Long cityId,
            @RequestParam(value = "townId", required = false) Long townId,
            HttpServletRequest request
    )
    {
        Authentication authentication = jwtTokenProvider.extractAuthentication(request);
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorCode.WRONG_USER));
        Long userId = user.getId();

        return allFriendsService.findUsersByRegion(provinceId, cityId, townId, userId);
    }

    @Operation(summary = "팔로우", description = "특정 사용자를 팔로우합니다.")
    @PostMapping("/follow")
    public ResponseEntity<String> followUser(@RequestParam Long userId, HttpServletRequest request) {
        Authentication authentication = jwtTokenProvider.extractAuthentication(request);
        String email = authentication.getName();

        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorCode.WRONG_USER));

        followFriendService.followUser(currentUser.getId(), userId);

        return ResponseEntity.ok("팔로우 완료 (userId=" + userId + ")");
    }


    @Operation(summary = "사용자 차단", description = "특정 사용자를 차단합니다.")
    @PostMapping("/block")
    public ResponseEntity<String> blockUser(@RequestParam Long userId, HttpServletRequest request) {
        Authentication authentication = jwtTokenProvider.extractAuthentication(request);
        String email = authentication.getName();

        User currentUser = userRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorCode.WRONG_USER));

        blockFriendService.blockUser(currentUser.getId(), userId);

        return ResponseEntity.ok("차단 완료 (userId=" + userId + ")");
    }

    @Operation(summary = "친구 검색", description = "키워드로 친구를 검색합니다.")
    @GetMapping("/search")
    public List<FriendsResponse> searchFriends(
            @RequestParam(value = "provinceId") Long provinceId,
            @RequestParam(value = "cityId", required = false) Long cityId,
            @RequestParam(value = "townId", required = false) Long townId,
            @RequestParam String keyword,
            HttpServletRequest request
    ) {
        Authentication authentication = jwtTokenProvider.extractAuthentication(request);
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorCode.WRONG_USER));
        Long userId = user.getId();

        return searchFriendsService.searchFriends(provinceId, cityId, townId, userId, keyword);
    }

    @Operation(summary = "사용자 신고", description = "특정 사용자를 신고합니다.")
    @PostMapping("/report")
    public ResponseEntity<String> reportUser(@RequestParam Long reportedId,
                                             @RequestBody ReasonRequest requestBody,
                                             HttpServletRequest request) {
        Authentication authentication = jwtTokenProvider.extractAuthentication(request);
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorCode.WRONG_USER));
        Long userId = user.getId();

        String reason = requestBody.getReason();

        reportService.sendReportToDiscord(userId, reportedId, reason);
        reportService.removeFollowRelation(userId, reportedId);

        return ResponseEntity.ok("신고가 접수되었습니다.");
    }

    // DTO 내부 클래스 또는 별도 파일로 분리 가능
    @Setter
    @Getter
    public static class ReasonRequest {
        private String reason;

    }




}
