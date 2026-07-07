package UMC_8th.With_Run.user.service;

import UMC_8th.With_Run.common.apiResponse.status.ErrorCode;
import UMC_8th.With_Run.common.exception.GeneralException;
import UMC_8th.With_Run.common.exception.handler.UserHandler;
import UMC_8th.With_Run.common.security.jwt.JwtTokenProvider;
import UMC_8th.With_Run.map.entity.RegionProvince;
import UMC_8th.With_Run.map.entity.RegionsCity;
import UMC_8th.With_Run.map.entity.RegionsTown;
import UMC_8th.With_Run.user.dto.UserRequestDto.UpdateNoticeSettingsDTO;
import UMC_8th.With_Run.user.dto.UserRequestDto.LoginRequestDTO;
import UMC_8th.With_Run.user.dto.UserRequestDto.RegionRequestDTO;
import UMC_8th.With_Run.user.dto.UserResponseDto;
import UMC_8th.With_Run.user.dto.UserResponseDto.LoginResultDTO;
import UMC_8th.With_Run.user.dto.UserResponseDto.RegionResponseDTO;
import UMC_8th.With_Run.user.entity.Profile;
import UMC_8th.With_Run.user.entity.User;
import UMC_8th.With_Run.user.repository.ProfileRepository;
import UMC_8th.With_Run.user.repository.RegionCityRepository;
import UMC_8th.With_Run.user.repository.RegionProvinceRepository;
import UMC_8th.With_Run.user.repository.RegionTownRepository;
import UMC_8th.With_Run.user.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.transaction.Transactional;
import java.util.Collections;
import java.util.Optional;

import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final RegionProvinceRepository provinceRepository;
    private final RegionCityRepository cityRepository;
    private final RegionTownRepository townRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    @Override
    public UserResponseDto.LoginResultDTO login(LoginRequestDTO request) {

        boolean isNewUser = false;

        // 이메일로 사용자 찾기
        Optional<User> userOptional = userRepository.findByEmailAndLoginId(request.getEmail(), request.getLoginId());

        User user;
        if (userOptional.isPresent()) {
            // 사용자가 존재하면 해당 정보를 사용합니다.
            user = userOptional.get();
        } else {
            // 사용자가 없으면 새로 생성하고 저장합니다.
            isNewUser = true; // 새로운 사용자이므로 플래그를 true로 설정
            User newUser = User.builder()
                    .email(request.getEmail())
                    .loginId(request.getLoginId())
                    .build();
            user = userRepository.save(newUser);
        }

        // 일단 패스워드 없이 이메일로만 로그인
        /*
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            throw new RuntimeException("비밀번호가 일치하지 않습니다.");
        }
        */

        String role = request.getEmail().equals("admin@naver.com") ? "ROLE_ADMIN" : "ROLE_USER";

        Authentication authentication = new UsernamePasswordAuthenticationToken(
                user.getEmail(), null,
                Collections.singleton(new SimpleGrantedAuthority(role))
        );

        String accessToken = jwtTokenProvider.generateToken(authentication);

        return LoginResultDTO.builder()
                .userId(user.getId())
                .accessToken(accessToken)
                .isNewUser(isNewUser)
                .build();
    }

    @Override
    @Transactional
    public void cancelMembership(HttpServletRequest request) {
        Authentication authentication = jwtTokenProvider.extractAuthentication(request);
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserHandler(ErrorCode.WRONG_USER));

        // soft delete
        user.delete();
        userRepository.save(user);

    }

    @Override
    public RegionResponseDTO setUserRegion(HttpServletRequest request, RegionRequestDTO dto) {
        Authentication authentication = jwtTokenProvider.extractAuthentication(request);
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserHandler(ErrorCode.WRONG_USER));

        Profile profile = user.getProfile();

        RegionProvince province = provinceRepository.findById(dto.getProvinceId())
                .orElseThrow(() -> new GeneralException(ErrorCode.BAD_REQUEST));

        RegionsCity city = null;
        if (dto.getCityId() != null) {
            city = cityRepository.findById(dto.getCityId())
                    .orElseThrow(() -> new GeneralException(ErrorCode.BAD_REQUEST));
        }

        RegionsTown town = null;
        if (dto.getTownId() != null) {
            town = townRepository.findById(dto.getTownId())
                    .orElseThrow(() -> new GeneralException(ErrorCode.BAD_REQUEST));
        }

        profile.setProvinceId(province.getId());
        profile.setCityId(city != null ? city.getId() : null);
        profile.setTownId(town != null ? town.getId() : null);

        profileRepository.save(profile);

        return RegionResponseDTO.builder()
                .province(RegionResponseDTO.RegionDTO.builder()
                        .id(province.getId())
                        .name(province.getName())
                        .build())
                .city(city != null ? RegionResponseDTO.RegionDTO.builder()
                        .id(city.getId()).name(city.getName()).build() : null)
                .town(town != null ? RegionResponseDTO.RegionDTO.builder()
                        .id(town.getId()).name(town.getName()).build() : null)
                .build();
    }

    @Transactional
    @Override
    public void updateNoticeSettings(HttpServletRequest request, UpdateNoticeSettingsDTO dto) {

        Authentication authentication = jwtTokenProvider.extractAuthentication(request);
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UserHandler(ErrorCode.WRONG_USER));

        if(dto.getEnabled() != null){
            user.updateNoticeEnabled(dto.getEnabled());
        }

    }


}

