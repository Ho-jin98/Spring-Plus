package org.example.expert.domain.user.service;

import lombok.RequiredArgsConstructor;
import org.example.expert.domain.user.dto.response.UserSearchResponse;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class UserCacheService {

    private final RedisTemplate<String, Object> redisTemplate;
    private static final String USER_NICKNAME = "users:nickname:";

    // 캐시 조회
    public List<UserSearchResponse> getUserNicknameCache(String nickname) {
        String key = USER_NICKNAME + nickname;
        return (List<UserSearchResponse>)redisTemplate.opsForValue().get(key);
    }

    // 캐시 저장
    public void saveUserNicknameCache(String nickname, List<UserSearchResponse> users) {
        String key = USER_NICKNAME + nickname;
        // 캐시 저장 시간을 1분으로 설정 (테스트용)
        redisTemplate.opsForValue().set(key, users, 1, TimeUnit.MINUTES);
    }
}
