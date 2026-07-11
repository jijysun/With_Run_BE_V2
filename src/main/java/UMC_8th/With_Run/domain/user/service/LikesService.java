package UMC_8th.With_Run.domain.user.service;

import UMC_8th.With_Run.domain.user.dto.UserResponseDto.LikeListResultDTO;
import jakarta.servlet.http.HttpServletRequest;

public interface LikesService {
    LikeListResultDTO getLikesByCurrentUser(HttpServletRequest request);
}

