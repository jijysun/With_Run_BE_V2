package UMC_8th.With_Run.map.controller;

import UMC_8th.With_Run.common.apiResponse.StndResponse;
import UMC_8th.With_Run.common.apiResponse.status.SuccessCode;
import UMC_8th.With_Run.map.dto.MapRequestDTO;
import UMC_8th.With_Run.map.dto.MapResponseDTO;
import UMC_8th.With_Run.map.service.CourseService;
import UMC_8th.With_Run.map.service.MapService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import UMC_8th.With_Run.common.apiResponse.status.ErrorCode;
import UMC_8th.With_Run.common.exception.GeneralException;
import UMC_8th.With_Run.common.security.jwt.JwtTokenProvider;
import UMC_8th.With_Run.user.entity.User;
import UMC_8th.With_Run.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "지도 API", description = "Swagger 테스트용 지도 관련 API")
@RestController
@RequestMapping("/api/maps")
@RequiredArgsConstructor
public class MapController {

    private final MapService mapService;
    private final CourseService courseService;
    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;

    // 수정: 위치 정보를 요청 파라미터에 추가했습니다.
    @Operation(
            summary = "카테고리 기반 반려동물 시설 검색",
            description = "카테고리와 위치를 기준으로 반려동물 시설을 페이징 형태로 검색합니다. 전체(또는 null) 검색 가능",
            parameters = {
                    @Parameter(name = "type", description = "카테고리 타입 (예: 동물약국)", required = true, example = "동물약국"),
                    @Parameter(name = "region_province", description = "시/도", required = true, example = "서울"),
                    @Parameter(name = "regions_city", description = "시/군/구", required = false, example = "강남구"),
                    @Parameter(name = "regions_town", description = "읍/면/동", required = false, example = "개포동"),
                    @Parameter(name = "page", description = "페이지 번호 (0부터 시작, 최소 0)", required = false, example = "0"),
                    @Parameter(name = "size", description = "한 페이지당 항목 수 (1-100)", required = false, example = "20")
            }
    )
    @GetMapping("/search/categories")
    public StndResponse<MapResponseDTO.PetFacilityPageResponseDto> searchByCategory(
            @RequestParam String type,
            @RequestParam(required = false) String region_province,
            @RequestParam(required = false) String regions_city,
            @RequestParam(required = false) String regions_town,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {

        // 수정: mapService 메서드 호출 시 위치 정보 파라미터를 추가했습니다.
        MapResponseDTO.PetFacilityPageResponseDto result =
                mapService.getPetFacilityByCategory(type, region_province, regions_city, regions_town, page, size);

        return StndResponse.onSuccess(result, SuccessCode.INQUIRY_SUCCESS);
    }


    @Operation(
            summary = "ID 기반 반려동물 시설 검색",
            description = "ID로 특정 반려동물 시설의 상세 정보를 검색합니다.",
            parameters = {
                    @Parameter(name = "id", description = "조회할 반려동물 시설의 ID", required = true, example = "1")
            }
    )
    @GetMapping("/search/{id}")
    public StndResponse<MapResponseDTO.PetFacilityResponseDto> getPetFacilityById(@PathVariable Long id) {
        MapResponseDTO.PetFacilityResponseDto facility = mapService.getPetFacilityById(id, null, null, null);
        return StndResponse.onSuccess(facility, SuccessCode.INQUIRY_SUCCESS);
    }


    @Operation(
            summary = "핀 생성",
            description = "새로운 핀을 생성합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "핀 생성 요청 DTO", required = true)
    )
    @PostMapping("/pin")
    public StndResponse<MapResponseDTO.PinResponseDto> createPin(@RequestBody @Valid MapRequestDTO.PinRequestDto requestDto) {
        Long pinId = mapService.createPin(requestDto);

        MapResponseDTO.PinResponseDto responseDto = MapResponseDTO.PinResponseDto.builder()
                .pinId(pinId)
                .build();

        return StndResponse.onSuccess(responseDto, SuccessCode.REQUEST_SUCCESS);
    }

    @Operation(
            summary = "핀 단건 조회",
            description = "핀 ID로 특정 핀 정보를 조회합니다.",
            parameters = {
                    @Parameter(name = "pinId", description = "조회할 핀의 ID", required = true, example = "1")
            }
    )
    @GetMapping("/pin/{pinId}")
    public StndResponse<MapResponseDTO.GetPinDto> getPinById(@PathVariable Long pinId) {
        MapResponseDTO.GetPinDto responseDto = mapService.getPinById(pinId);

        return StndResponse.onSuccess(responseDto, SuccessCode.INQUIRY_SUCCESS);
    }

    @Operation(
            summary = "핀 수정",
            description = "기존 핀 정보를 수정합니다.",
            parameters = {
                    @Parameter(name = "pinId", description = "수정할 핀의 ID", required = true, example = "1")
            }
    )
    @PatchMapping("/pin/{pinId}")
    public StndResponse<MapResponseDTO.PinResponseDto> updatePin(@PathVariable Long pinId, @RequestBody @Valid MapRequestDTO.PinRequestDto requestDto) {
        Long updatedPinId = mapService.updatePin(pinId, requestDto);

        MapResponseDTO.PinResponseDto responseDto = MapResponseDTO.PinResponseDto.builder()
                .pinId(updatedPinId)
                .build();

        return StndResponse.onSuccess(responseDto, SuccessCode.UPDATE_SUCCESS);
    }

    @Operation(
            summary = "핀 삭제",
            description = "핀을 삭제합니다.",
            parameters = {
                    @Parameter(name = "pinId", description = "삭제할 핀의 ID", required = true, example = "1")
            }
    )
    @DeleteMapping("/pin/{pinId}")
    public StndResponse<MapResponseDTO.PinResponseDto> deletePin(@PathVariable Long pinId) {
        Long deletedPinId = mapService.deletePin(pinId);

        MapResponseDTO.PinResponseDto responseDto = MapResponseDTO.PinResponseDto.builder()
                .pinId(deletedPinId)
                .build();

        return StndResponse.onSuccess(responseDto, SuccessCode.DELETE_SUCCESS);
    }


    //코스 관련 API
    /*@Operation(
            summary = "산책 코스 생성",
            description = "산책 코스를 등록합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "산책 코스 생성 요청 DTO", required = true)
    )
    @PostMapping("/courses")
    public StndResponse<MapResponseDTO.CourseCreateResponseDto> createCourse(
            @RequestBody @Valid MapRequestDTO.CourseCreateRequestDto requestDto,
            HttpServletRequest request) {
        
        // CourseController와 동일한 방식으로 사용자 인증 정보 추출
        Authentication authentication = jwtTokenProvider.extractAuthentication(request);
        String email = authentication.getName();
        
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorCode.WRONG_USER));
        
        Long courseId = courseService.createCourse(user.getId(), requestDto);
        MapResponseDTO.CourseCreateResponseDto response = MapResponseDTO.CourseCreateResponseDto.builder()
                .courseId(courseId)
                .build();
        return StndResponse.onSuccess(response, SuccessCode.REQUEST_SUCCESS);
    }
*/

    @Operation(
            summary = "산책 코스 생성",
            description = "산책 코스를 등록합니다.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "산책 코스 생성 요청 DTO", required = true)
    )
    @PostMapping(value = "/courses", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public StndResponse<MapResponseDTO.CourseCreateResponseDto> createCourse(
            @RequestPart("courseCreateRequest") String courseCreateRequestJson,
            @RequestPart(value = "courseImg", required = false) MultipartFile courseImg,
            HttpServletRequest request) throws JsonProcessingException {

        // CourseController와 동일한 방식으로 사용자 인증 정보 추출
        Authentication authentication = jwtTokenProvider.extractAuthentication(request);
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new GeneralException(ErrorCode.WRONG_USER));


        ObjectMapper objectMapper = new ObjectMapper();
        MapRequestDTO.CourseCreateRequestDto courseDTO = objectMapper.readValue(courseCreateRequestJson, MapRequestDTO.CourseCreateRequestDto.class);

        Long courseId = courseService.createCourseV2(user.getId(), courseDTO, courseImg);
        MapResponseDTO.CourseCreateResponseDto response = MapResponseDTO.CourseCreateResponseDto.builder()
                .courseId(courseId)
                .build();
        return StndResponse.onSuccess(response, SuccessCode.REQUEST_SUCCESS);
    }


}