package UMC_8th.With_Run.user.service;

import UMC_8th.With_Run.user.dto.UserResponseDto.FollowerListResultDTO;
import UMC_8th.With_Run.user.dto.UserResponseDto.FollowingListResultDTO;
import jakarta.servlet.http.HttpServletRequest;

public interface FollowService {
    FollowingListResultDTO getFollowingList(HttpServletRequest request);
    FollowerListResultDTO getFollowerList(HttpServletRequest request);
    void cancelFollowing(Long targetUserId, HttpServletRequest request);
    void deleteFollower(Long followerId, HttpServletRequest request);
}

