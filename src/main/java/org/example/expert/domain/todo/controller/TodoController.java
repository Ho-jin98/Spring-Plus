package org.example.expert.domain.todo.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.todo.dto.request.TodoSaveRequest;
import org.example.expert.domain.todo.dto.response.TodoResponse;
import org.example.expert.domain.todo.dto.response.TodoSaveResponse;
import org.example.expert.domain.todo.dto.response.TodoSearchResponse;
import org.example.expert.domain.todo.service.TodoService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequiredArgsConstructor
public class TodoController {

    private final TodoService todoService;

    @PostMapping("/todos")
    public ResponseEntity<TodoSaveResponse> saveTodo(
            @AuthenticationPrincipal AuthUser authUser,
            @Valid @RequestBody TodoSaveRequest todoSaveRequest
    ) {
        return ResponseEntity.ok(todoService.saveTodo(authUser, todoSaveRequest));
    }

    @GetMapping("/todos")
    public ResponseEntity<Page<TodoResponse>> getTodos(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size,
            // 날씨 조건은 있을 수도 없을 수도 있으므로 required = false 활용,
            // 날씨 기간(시작일, 종료일), 날씨 정보를 파라미터에 추가
            @RequestParam(required = false) String weather,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate
    ) {
        return ResponseEntity.ok(todoService.getTodos(page -1 , size, weather, startDate, endDate));
    }

    @GetMapping("/todos/{todoId}")
    public ResponseEntity<TodoResponse> getTodo(@PathVariable long todoId) {
        return ResponseEntity.ok(todoService.getTodo(todoId));
    }

    @GetMapping("/todos/search")
    public ResponseEntity<Page<TodoSearchResponse>> getTodoSearch(
            @RequestParam(required = false) String title,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(required = false) LocalDateTime endDate,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Pageable pageable = PageRequest.of(page -1 , size);
        return ResponseEntity.ok((todoService.searchTodos(title, nickname, startDate, endDate, pageable)));
    }
}

/* 복습
   @PathVariable vs @RequestParam
   둘다 URL로 받는 방식은 맞지만 약간 차이가 있음,
   1. @PathVariable -> URL에 직접 명시를 해줘야함
   -> ex) ("/todos/{todoId}"), GET /todos/1

   2. @RequestParam -> 쿼리 스트링을 받는 어노테이션인데, ?뒤에 붙는 값들은 URL에 명시 안해줘도됨
   -> ex) ("/todos/search"), GET /todos/search?title=제목&nickname=홍길동*/
