package UMC_8th.With_Run.user.service;

import UMC_8th.With_Run.common.apiResponse.status.ErrorCode;
import UMC_8th.With_Run.common.exception.GeneralException;
import UMC_8th.With_Run.common.exception.handler.UserHandler;
import UMC_8th.With_Run.common.security.jwt.JwtTokenProvider;
import UMC_8th.With_Run.user.dto.UserResponseDto.FollowItemDTO;
import UMC_8th.With_Run.user.dto.UserResponseDto.FollowerItemDTO;
import UMC_8th.With_Run.user.dto.UserResponseDto.FollowerListResultDTO;
import UMC_8th.With_Run.user.dto.UserResponseDto.FollowingListResultDTO;
import UMC_8th.With_Run.user.entity.Follow;
import UMC_8th.With_Run.user.entity.Profile;
import UMC_8th.With_Run.user.entity.User;
import UMC_8th.With_Run.user.repository.FollowRepository;
import UMC_8th.With_Run.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class FollowServiceImpl implements FollowService {

    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public FollowingListResultDTO getFollowingList(HttpServletRequest request) {
        Authentication authentication = jwtTokenProvider.extractAuthentication(request);
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserHandler(ErrorCode.WRONG_USER));

        List<Follow> followings = followRepository.findAllByUserId(user.getId());

        List<FollowItemDTO> result = followings.stream()
                .map(follow -> {
                    User targetUser = follow.getTargetUser();
                    Profile profile = targetUser.getProfile();

                    return FollowItemDTO.builder()
                            .targetUserId(targetUser.getId())
                            .name(profile != null ? profile.getName() : null)
                            .profileImage(profile != null ? profile.getProfileImage() : null)
                            .build();
                })
                .toList();

        return FollowingListResultDTO.builder()
                .followings(result)
                .count(result.size())
                .build();
    }

    @Override
    public FollowerListResultDTO getFollowerList(HttpServletRequest request) {
        Authentication authentication = jwtTokenProvider.extractAuthentication(request);
        String email = authentication.getName();

        User me = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserHandler(ErrorCode.WRONG_USER));

        List<Follow> followers = followRepository.findAllByTargetUserId(me.getId());

        List<FollowerItemDTO> result = followers.stream()
                .map(f -> {
                    User follower = f.getUser();
                    Profile profile = follower.getProfile();

                    return FollowerItemDTO.builder()
                            .userId(follower.getId())
                            .name(profile != null ? profile.getName() : null)
                            .profileImage(profile != null ? profile.getProfileImage() : null)
                            .build();
                })
                .toList();

        return FollowerListResultDTO.builder()
                .followers(result)
                .count(result.size())
                .build();
    }


    @Override
    public void cancelFollowing(Long targetUserId, HttpServletRequest request) {
        Authentication authentication = jwtTokenProvider.extractAuthentication(request);
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserHandler(ErrorCode.WRONG_USER));

        // 본인이 자기 자신을 unfollow 시도하는 경우 방지
        if (user.getId().equals(targetUserId)) {
            throw new GeneralException(ErrorCode.BAD_REQUEST);
        }

        Follow follow = followRepository.findByUserIdAndTargetUserId(user.getId(), targetUserId)
                .orElseThrow(() -> new GeneralException(ErrorCode.BAD_REQUEST));

        followRepository.delete(follow);
    }

    @Override
    public void deleteFollower(Long followerId, HttpServletRequest request) {
        Authentication authentication = jwtTokenProvider.extractAuthentication(request);
        String myEmail = authentication.getName();

        User me = userRepository.findByEmail(myEmail)
                .orElseThrow(() -> new GeneralException(ErrorCode.WRONG_USER));

        User follower = userRepository.findById(followerId)
                .orElseThrow(() -> new GeneralException(ErrorCode.WRONG_USER));

        Follow follow = followRepository.findByUserIdAndTargetUserId(follower.getId(), me.getId())
                .orElseThrow(() -> new GeneralException(ErrorCode.BAD_REQUEST));

        followRepository.delete(follow);
    }



}

