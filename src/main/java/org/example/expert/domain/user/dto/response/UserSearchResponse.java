package org.example.expert.domain.user.dto.response;

import lombok.Getter;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;

@Getter
public class UserSearchResponse {

    private Long id;
    private String email;
    private UserRole userRole;
    private String nickname;

    private UserSearchResponse(Long id, String email, UserRole userRole, String nickname) {
        this.id = id;
        this.email = email;
        this.userRole = userRole;
        this.nickname = nickname;
    }

    public static UserSearchResponse from(User user) {
        return new UserSearchResponse(
                user.getId(),
                user.getEmail(),
                user.getUserRole(),
                user.getNickname()
        );
    }
}
