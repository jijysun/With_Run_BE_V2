package UMC_8th.With_Run.user.service;

import UMC_8th.With_Run.user.dto.UserRequestDto.UpdateNoticeSettingsDTO;
import UMC_8th.With_Run.user.dto.UserRequestDto.LoginRequestDTO;
import UMC_8th.With_Run.user.dto.UserRequestDto.RegionRequestDTO;
import UMC_8th.With_Run.user.dto.UserResponseDto;
import UMC_8th.With_Run.user.dto.UserResponseDto.RegionResponseDTO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;

public interface UserService {
    UserResponseDto.LoginResultDTO login(LoginRequestDTO loginRequestDTO);
    void cancelMembership(HttpServletRequest request);

    RegionResponseDTO setUserRegion(HttpServletRequest request, RegionRequestDTO dto);

    @Transactional
    void updateNoticeSettings(HttpServletRequest request, UpdateNoticeSettingsDTO dto);
}
