package org.example.expert.domain.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.expert.config.PasswordEncoder;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.user.dto.request.UserChangePasswordRequest;
import org.example.expert.domain.user.dto.response.UserResponse;
import org.example.expert.domain.user.dto.response.UserSearchResponse;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    // 캐시 서비스 주입
    private final UserCacheService userCacheService;

    public UserResponse getUser(long userId) {
        User user = userRepository.findById(userId).orElseThrow(() -> new InvalidRequestException("User not found"));
        return new UserResponse(user.getId(), user.getEmail());
    }

    @Transactional
    public void changePassword(long userId, UserChangePasswordRequest userChangePasswordRequest) {
        validateNewPassword(userChangePasswordRequest);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new InvalidRequestException("User not found"));

        if (passwordEncoder.matches(userChangePasswordRequest.getNewPassword(), user.getPassword())) {
            throw new InvalidRequestException("새 비밀번호는 기존 비밀번호와 같을 수 없습니다.");
        }

        if (!passwordEncoder.matches(userChangePasswordRequest.getOldPassword(), user.getPassword())) {
            throw new InvalidRequestException("잘못된 비밀번호입니다.");
        }

        user.changePassword(passwordEncoder.encode(userChangePasswordRequest.getNewPassword()));
    }

    private static void validateNewPassword(UserChangePasswordRequest userChangePasswordRequest) {
        if (userChangePasswordRequest.getNewPassword().length() < 8 ||
                !userChangePasswordRequest.getNewPassword().matches(".*\\d.*") ||
                !userChangePasswordRequest.getNewPassword().matches(".*[A-Z].*")) {
            throw new InvalidRequestException("새 비밀번호는 8자 이상이어야 하고, 숫자와 대문자를 포함해야 합니다.");
        }
    }

/*     JPA (findByNickname)으로 속도 측정
    public List<UserSearchResponse> searchNickname(String nickname) {
        return userRepository.findByNickname(nickname).stream()
                // User -> UserSearchResponse DTO로 변환
                .map(UserSearchResponse::from)
                .toList();
    }*/

    // Redis 캐시 사용해서 속도 측정

    public List<UserSearchResponse> searchNickname(String nickname) {

        List<UserSearchResponse> cached = userCacheService.getUserNicknameCache(nickname);
        if (cached != null) {
            log.info("Redis Data Cache Hit");
            return cached;
        }

        log.info("Redis Data Cache Miss");
        // 캐시에 데이터가 없으면 DB에 직접 조회
        List<UserSearchResponse> result = userRepository.findByNickname(nickname).stream()
                // User -> UserSearchResponse DTO로 변환
                .map(UserSearchResponse::from)
                .toList();

        userCacheService.saveUserNicknameCache(nickname, result);
        return result;
    }
}
