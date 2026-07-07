package UMC_8th.With_Run.user.service;

import UMC_8th.With_Run.user.dto.UserResponseDto.ScrapListResultDTO;
import jakarta.servlet.http.HttpServletRequest;

public interface ScrapService {
    ScrapListResultDTO getScrapsByCurrentUser(HttpServletRequest request);
}
