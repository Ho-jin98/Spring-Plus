package org.example.expert.domain.common.dto;

import lombok.Getter;
import org.example.expert.domain.user.enums.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/* @AuthenticationPrincipal이 동작하기 위해서 Spring Security의 UserDetails를 구현

 AuthUser에 구현하는 이유
 1. AuthUser DTO -> 말 그대로 인증된 사용자 정보를 담는 DTO
 2. Spring Security는 인증된 사용자 정보를 UserDetails 타입으로 관리함,그래서 SecurityContextHolder에 저장하고,
    @AuthenticationPrincipal로 꺼낼 때 UserDetails를 구현한 객체여야 인식이 가능함!

    이해하기 쉽게 생각해보면, Spring Security가 UserDetails만 알아보기 때문에, AuthUser가 UserDetails를 구현해야지
    Security가 AuthUser를 인증 객체로 인식할 수 있다!!*/

@Getter
public class AuthUser implements UserDetails {

    private final Long id;
    private final String email;
    private final UserRole userRole;

    public AuthUser(Long id, String email, UserRole userRole) {
        this.id = id;
        this.email = email;
        this.userRole = userRole;
    }

    @Override
    /* getAuthorities() -> 사용자의 권한 목록을 반환, (ADMIN, USER) 같은 역할을
     GrantedAuthority로 변환해서 반환, Security가 권한 체크할 때 여기서 가져온다.*/
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority(userRole.name()));
    }

    // getPassword() -> 사용자 비밀번호 반환, AuthUser는 인증이 이미 완료된 객체라 비밀번호가 필요 없어서 null을 반환
    @Override
    public String getPassword() {return null;}

    /* getUsername() -> 사용자 식별자 반환, 여기서는 email을 식별자로 사용
     Username이라고 되어 있지만 실제로 유저의 이름이 아님!!
     Spring Security에서 사용자를 구분하는 식별자이다*/
    @Override
    public String getUsername() {return email;}

    // 이 외에도 UserDetails 인터페이스에는 다양한 메서드들이 들어 있음
}
