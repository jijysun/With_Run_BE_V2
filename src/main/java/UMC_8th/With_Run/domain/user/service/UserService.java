package UMC_8th.With_Run.domain.user.service;

import UMC_8th.With_Run.domain.user.dto.UserRequestDto.UpdateNoticeSettingsDTO;
import UMC_8th.With_Run.domain.user.dto.UserRequestDto.LoginRequestDTO;
import UMC_8th.With_Run.domain.user.dto.UserRequestDto.RegionRequestDTO;
import UMC_8th.With_Run.domain.user.dto.UserResponseDto;
import UMC_8th.With_Run.domain.user.dto.UserResponseDto.RegionResponseDTO;
import UMC_8th.With_Run.domain.user.entity.Profile;
import UMC_8th.With_Run.domain.user.entity.User;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;

public interface UserService {
    UserResponseDto.LoginResultDTO login(LoginRequestDTO loginRequestDTO);
    void cancelMembership(HttpServletRequest request);

    RegionResponseDTO setUserRegion(HttpServletRequest request, RegionRequestDTO dto);

    @Transactional
    void updateNoticeSettings(HttpServletRequest request, UpdateNoticeSettingsDTO dto);

    // chatting() 발신자 조회용 캐시 래퍼. User/Profile을 각각 다른 TTL로 캐싱하기 위해
    // 기존 findByEmailWithProfile(join fetch) 대신 이 둘로 분리해서 조회해야 핢
    User getCachedUser(String email);
    Profile getCachedProfile(Long userId);
}
