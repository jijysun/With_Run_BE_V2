package UMC_8th.With_Run.user.controller;

import UMC_8th.With_Run.common.apiResponse.StndResponse;
import UMC_8th.With_Run.common.apiResponse.status.SuccessCode;
import UMC_8th.With_Run.user.dto.UserRequestDto;
import UMC_8th.With_Run.user.dto.UserRequestDto.BreedProfileRequestDTO;
import UMC_8th.With_Run.user.dto.UserRequestDto.LoginRequestDTO;
import UMC_8th.With_Run.user.dto.UserRequestDto.ProfileImageRequest;
import UMC_8th.With_Run.user.dto.UserRequestDto.RegionRequestDTO;
import UMC_8th.With_Run.user.dto.UserRequestDto.UpdateCourseDTO;
import UMC_8th.With_Run.user.dto.UserRequestDto.UpdateProfileDTO;
import UMC_8th.With_Run.user.dto.UserResponseDto;
import UMC_8th.With_Run.user.dto.UserResponseDto.FollowerListResultDTO;
import UMC_8th.With_Run.user.dto.UserResponseDto.FollowingListResultDTO;
import UMC_8th.With_Run.user.dto.UserResponseDto.LikeListResultDTO;
import UMC_8th.With_Run.user.dto.UserResponseDto.ProfileResultDTO;
import UMC_8th.With_Run.user.dto.UserResponseDto.RegionResponseDTO;
import UMC_8th.With_Run.user.dto.UserResponseDto.ScrapListResultDTO;
import UMC_8th.With_Run.user.dto.UserResponseDto.SimpleUserResultDTO;
import UMC_8th.With_Run.user.service.FollowService;
import UMC_8th.With_Run.user.service.LikesService;
import UMC_8th.With_Run.user.service.MyCourseService;
import UMC_8th.With_Run.user.service.ProfileService;
import UMC_8th.With_Run.user.service.ScrapService;
import UMC_8th.With_Run.user.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "사용자 API")
public class UserController {

    private final UserService userService;
    private final ProfileService profileService;
    private final LikesService likesService;
    private final ScrapService scrapService;
    private final FollowService followService;
    private final MyCourseService myCourseService;

    @PostMapping("/login")
    @Operation(summary = "로그인 API", description = "로그인 API입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "로그인 성공", content = @Content(schema = @Schema(implementation = StndResponse.class)))
    })
    public StndResponse<UserResponseDto.LoginResultDTO> login(@RequestBody LoginRequestDTO request) {
        UserResponseDto.LoginResultDTO dto = userService.login(request);  // request 전달
        return StndResponse.onSuccess(dto, SuccessCode.LOGIN_SUCCESS);
    }

    @PostMapping("/profile")
    @Operation(summary = "반려견 프로필 설정 API", description = "반려견의 프로필 정보를 설정하는 API입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "프로필 생성 성공", content = @Content(schema = @Schema(implementation = StndResponse.class))),
    })
    public StndResponse<BreedProfileRequestDTO> createBreedProfile(
            @RequestBody BreedProfileRequestDTO requestDTO,
            HttpServletRequest request
    ) {
        BreedProfileRequestDTO result = profileService.createBreedProfile(requestDTO, request);
        return StndResponse.onSuccess(result, SuccessCode.REQUEST_SUCCESS);
    }

    @PostMapping(value = "/profile/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "프로필 이미지 업로드 API", description = "반려견의 프로필 이미지를 업로드합니다.")
    public StndResponse<String> uploadProfileImage(
            @ModelAttribute ProfileImageRequest request,
            HttpServletRequest servletRequest
    ) throws IOException {
        String result = profileService.uploadProfileImage(request.getFile(), servletRequest);
        return StndResponse.onSuccess(result, SuccessCode.REQUEST_SUCCESS);
    }

    @PostMapping("/region")
    @Operation(summary = "동네 설정 API", description = "사용자의 동네 정보를 설정하는 API입니다.")
    public StndResponse<RegionResponseDTO> createRegion(
            @RequestBody @Valid RegionRequestDTO regionRequestDTO,
            HttpServletRequest request) {

        RegionResponseDTO response = userService.setUserRegion(request, regionRequestDTO);
        return StndResponse.onSuccess(response, SuccessCode.REQUEST_SUCCESS);
    }


    @PatchMapping("/alarm")
    @Operation(summary = "알림 설정 API", description = "사용자의 전체 알림 수신 여부를 설정하는 API입니다. true로 보내면 켜기, false는 끄기입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공", content = @Content(schema = @Schema(implementation = StndResponse.class)))
    })
    public StndResponse<SimpleUserResultDTO> updateAlarmSettings(
            HttpServletRequest request,
            @RequestBody UserRequestDto.UpdateNoticeSettingsDTO updateNoticeSettingsDTO) {

        userService.updateNoticeSettings(request, updateNoticeSettingsDTO);
        return StndResponse.onSuccess(
                new SimpleUserResultDTO("알림 설정이 완료되었습니다."),
                SuccessCode.REQUEST_SUCCESS
        );
    }

    @PatchMapping("/")
    @Operation(summary = "회원 탈퇴 API", description = "JWT 토큰을 바탕으로 본인의 계정을 탈퇴합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "탈퇴 성공", content = @Content(schema = @Schema(implementation = StndResponse.class)))
    })
    public StndResponse<SimpleUserResultDTO> cancelMembership(HttpServletRequest request) {
        userService.cancelMembership(request);
        return StndResponse.onSuccess(
                new SimpleUserResultDTO("탈퇴가 완료되었습니다."),
                SuccessCode.REQUEST_SUCCESS
        );
    }

    @PostMapping("/logout")
    @Operation(summary = "로그아웃 API", description = "로그아웃 API입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "TestSuccessCode", content = @Content(schema = @Schema(implementation = StndResponse.class)))
    })
    @Parameters({
            @Parameter(name = "userId", description = "사용자 id 입니다.")
    })
    public SuccessCode logout(){
        return SuccessCode.REQUEST_SUCCESS;
    }

    @GetMapping("/profile")
    @Operation(summary = "프로필 조회 API", description = "사용자의 기본 프로필을 조회하는 API입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공")
    })
    public StndResponse<ProfileResultDTO> getProfile(HttpServletRequest request){
        UserResponseDto.ProfileResultDTO dto = profileService.getProfileByCurrentUser(request);
        return StndResponse.onSuccess(dto, SuccessCode.INQUIRY_SUCCESS);
    }

    @GetMapping("/scraps")
    @Operation(summary = "스크랩 목록 조회 API", description = "사용자의 스크랩 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = StndResponse.class)))
    })
    public StndResponse<ScrapListResultDTO> getScrapList(HttpServletRequest request) {
        ScrapListResultDTO dto = scrapService.getScrapsByCurrentUser(request);
        return StndResponse.onSuccess(dto, SuccessCode.INQUIRY_SUCCESS);
    }

    @GetMapping("/likes")
    @Operation(summary = "좋아요 목록 조회 API", description = "사용자의 좋아요한 코스 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = StndResponse.class)))
    })
    public StndResponse<LikeListResultDTO> getLikeList(HttpServletRequest request) {
        LikeListResultDTO dto = likesService.getLikesByCurrentUser(request);
        return StndResponse.onSuccess(dto, SuccessCode.INQUIRY_SUCCESS);
    }


    @GetMapping("/courses")
    @Operation(summary = "내 코스 목록 조회 API", description = "현재 로그인한 사용자가 작성한 코스를 조회하는 API입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = StndResponse.class)))
    })
    public StndResponse<UserResponseDto.MyCourseListResultDTO> getCourseList(HttpServletRequest request) {
        UserResponseDto.MyCourseListResultDTO dto = myCourseService.getMyCourses(request);
        return StndResponse.onSuccess(dto, SuccessCode.INQUIRY_SUCCESS);
    }


    @GetMapping("/followers")
    @Operation(summary = "팔로워 목록 조회 API", description = "나를 팔로우한 사용자 목록(팔로워 리스트)을 반환합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = StndResponse.class)))
    })
    public StndResponse<FollowerListResultDTO> getFollowerList(HttpServletRequest request) {
        FollowerListResultDTO dto = followService.getFollowerList(request);
        return StndResponse.onSuccess(dto, SuccessCode.INQUIRY_SUCCESS);
    }

    @GetMapping("/followings")
    @Operation(summary = "팔로우 목록 조회 API", description = "현재 사용자의 팔로잉 목록을 조회합니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "조회 성공", content = @Content(schema = @Schema(implementation = StndResponse.class)))
    })
    public StndResponse<FollowingListResultDTO> getFollowingList(HttpServletRequest request) {
        FollowingListResultDTO dto = followService.getFollowingList(request);
        return StndResponse.onSuccess(dto, SuccessCode.INQUIRY_SUCCESS);
    }


    @DeleteMapping("/followings/{following_id}")
    @Operation(summary = "팔로잉 취소 API", description = "사용자가 팔로우를 취소하는 API입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "요청 성공")
    })
    public StndResponse<SimpleUserResultDTO> cancelFollowing(
            @PathVariable("following_id") Long followingId,
            HttpServletRequest request
    ) {
        followService.cancelFollowing(followingId, request);
        return StndResponse.onSuccess(
                new SimpleUserResultDTO("id " + followingId + "의 팔로우를 취소하였습니다."),
                SuccessCode.REQUEST_SUCCESS
        );
    }


    @DeleteMapping("/followers/{follower_id}")
    @Operation(summary = "팔로워 삭제 API", description = "사용자의 팔로워를 삭제하는 API입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "삭제 성공", content = @Content(schema = @Schema(implementation = StndResponse.class)))
    })
    public StndResponse<SimpleUserResultDTO> deleteFollower(
            @PathVariable("follower_id") Long followerId,
            HttpServletRequest request
    ) {
        followService.deleteFollower(followerId, request);
        return StndResponse.onSuccess(
                new SimpleUserResultDTO("id " + followerId + " 팔로워를 삭제하였습니다."),
                SuccessCode.REQUEST_SUCCESS
        );
    }


    @PatchMapping("/profile")
    @Operation(summary = "프로필 수정 API", description = "사용자의 프로필을 수정하는 API입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "성공", content = @Content(schema = @Schema(implementation = StndResponse.class)))
    })
    public StndResponse<UpdateProfileDTO> updateProfile(
            @RequestBody UpdateProfileDTO updateProfileDTO,
            HttpServletRequest request
    ) {
        UpdateProfileDTO result = profileService.updateProfile(updateProfileDTO, request);
        return StndResponse.onSuccess(result, SuccessCode.REQUEST_SUCCESS);
    }


    @PatchMapping("/courses/{course_id}")
    @Operation(summary = "코스 수정 API", description = "사용자의 코스를 수정하는 API입니다.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "수정 성공", content = @Content(schema = @Schema(implementation = StndResponse.class)))
    })
    public StndResponse<UpdateCourseDTO> updateCourse(
            @PathVariable("course_id") Long courseId,
            @RequestBody UpdateCourseDTO updateCourseDTO
    ) {
        UpdateCourseDTO updatedDto = myCourseService.updateCourse(courseId, updateCourseDTO);
        return StndResponse.onSuccess(updatedDto, SuccessCode.REQUEST_SUCCESS);
    }


}
