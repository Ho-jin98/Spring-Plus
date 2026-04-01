package org.example.expert.domain.todo.controller;

import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.todo.dto.response.TodoResponse;
import org.example.expert.domain.todo.service.TodoService;
import org.example.expert.domain.user.dto.response.UserResponse;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import javax.swing.*;
import java.time.LocalDateTime;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TodoController.class)
class TodoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private TodoService todoService;

    @Test
    void todo_단건_조회에_성공한다() throws Exception {
        // given
        long todoId = 1L;
        String title = "title";
        AuthUser authUser = new AuthUser(1L, "email", UserRole.USER);
        User user = User.fromAuthUser(authUser);
        UserResponse userResponse = new UserResponse(user.getId(), user.getEmail());
        TodoResponse response = new TodoResponse(
                todoId,
                title,
                "contents",
                "Sunny",
                userResponse,
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        // when
        when(todoService.getTodo(todoId)).thenReturn(response);

        // then
        mockMvc.perform(get("/todos/{todoId}", todoId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(todoId))
                .andExpect(jsonPath("$.title").value(title));
    }

    @Test
    void todo_단건_조회_시_todo가_존재하지_않아_예외가_발생한다() throws Exception {
        // given
        long todoId = 1L;

        // when
        when(todoService.getTodo(todoId))
                .thenThrow(new InvalidRequestException("Todo not found"));

        // then
        mockMvc.perform(get("/todos/{todoId}", todoId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.name()))
                .andExpect(jsonPath("$.code").value(HttpStatus.BAD_REQUEST.value()))
                .andExpect(jsonPath("$.message").value("Todo not found"));
    }
}
/*
 given -> 테스트 사전 조건 설정, "~가 주어졌을 때" 즉 테스트에 필요한 데이터나 mock 설정을 준비하는 단계
 when -> 실제 테스트할 동작을 실행하는 단계, "~을 실행했을 때" 즉, 메서드를 호출하는 단계
 then -> 결과 겸증 단계, "~이어야 한다." 주장함, 즉 실행 결과가 예상과 맞는지 확인하는 단계

 build.gradle에 security 의존성 추가가 되어 있으면 400이 안떨어지고 401로 떨어지려함

 MockMvc에서 Security필터를 비활성화하는 어노테이션
 Spring Security도 필터 기반으로 동작하기 때문에 이 설정으로 Security 인증 과정을 건너뛸 수 있음.
 @AutoConfigureMockMvc(addFilters = false) */
