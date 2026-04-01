package org.example.expert.config;

import lombok.RequiredArgsConstructor;
import org.example.expert.domain.common.dto.AuthUser;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@RequiredArgsConstructor
@EnableWebSecurity
public class SecurityConfig {

    private final JwtFilter jwtFilter;

    @Bean
    public SecurityFilterChain SecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
                .authorizeHttpRequests(auth -> auth
                        // 여기서 회원가입, 로그인은 permitAll()로 열어줬다는 것을 잊지 말자
                        // -> 포스트맨에서 테스트시 회원가입과 로그인은, permitAll()로 열어놨기 때문에
                        // 토큰 넣지 않거나, 이상한 토큰으로 Send를 눌렀을 때  200OK 가 떨어짐
                        .requestMatchers("/auth/**").permitAll()
                        .requestMatchers("/admin/**").hasAnyAuthority("ADMIN")
                        .anyRequest().authenticated());
        return http.build();

        /* csrf, httpBasic, formLogin 비활성화
         JwtFilter 를 UsernamePasswordAuthenticationFilter 앞에 등록
         /auth*//** : 누구나 접근 가능 -> 회원가입, 로그인
         /admin/** : ADMIN 권한만 접근 가능*/
    }
}
/* 전체적인 흐름
  요청이 오면
 → JwtFilter (토큰 검증 → SecurityContextHolder에 AuthUser 저장)
 → SecurityConfig (URL별 권한 체크)
 → Controller (@AuthenticationPrincipal로 AuthUser 꺼내서 사용)
   기존에는 FilterConfig -> JwtFilter -> AuthUserArgumentResolver 순서로 처리를 했는데,
   이제 Spring Security가 그 역할을 대신 해줌!
 */

/* 역할이 어떻게 분배되었는지 정리
    AuthUserArgumentResolver → @AuthenticationPrincipal로 대체
    FilterConfig → SecurityConfig의 addFilterBefore로 대체
    WebConfig → AuthUserArgumentResolver 삭제했으니 필요 없어짐*/
