package UMC_8th.With_Run.user.service;

import UMC_8th.With_Run.user.dto.UserResponseDto.LikeListResultDTO;
import jakarta.servlet.http.HttpServletRequest;

public interface LikesService {
    LikeListResultDTO getLikesByCurrentUser(HttpServletRequest request);
}

