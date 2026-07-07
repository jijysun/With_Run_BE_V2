package UMC_8th.With_Run.user.service;

import UMC_8th.With_Run.user.dto.UserRequestDto.BreedProfileRequestDTO;
import UMC_8th.With_Run.user.dto.UserRequestDto.UpdateProfileDTO;
import UMC_8th.With_Run.user.dto.UserResponseDto;
import jakarta.servlet.http.HttpServletRequest;
import java.io.IOException;
import org.springframework.web.multipart.MultipartFile;

public interface ProfileService {
    UserResponseDto.ProfileResultDTO getProfileByCurrentUser(HttpServletRequest request);
    BreedProfileRequestDTO createBreedProfile(BreedProfileRequestDTO requestDTO, HttpServletRequest request);
    UpdateProfileDTO updateProfile(UpdateProfileDTO dto, HttpServletRequest request);
    String uploadProfileImage(MultipartFile file, HttpServletRequest request) throws IOException;
}
