package org.example.expert.domain.todo.service;

import lombok.RequiredArgsConstructor;
import org.example.expert.client.WeatherClient;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.todo.dto.request.TodoSaveRequest;
import org.example.expert.domain.todo.dto.response.TodoResponse;
import org.example.expert.domain.todo.dto.response.TodoSaveResponse;
import org.example.expert.domain.todo.dto.response.TodoSearchResponse;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.todo.repository.TodoRepository;
import org.example.expert.domain.user.dto.response.UserResponse;
import org.example.expert.domain.user.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TodoService {

    private final TodoRepository todoRepository;
    private final WeatherClient weatherClient;

    @Transactional
    public TodoSaveResponse saveTodo(AuthUser authUser, TodoSaveRequest todoSaveRequest) {
        User user = User.fromAuthUser(authUser);

        String weather = weatherClient.getTodayWeather();

        Todo newTodo = new Todo(
                todoSaveRequest.getTitle(),
                todoSaveRequest.getContents(),
                weather,
                user
        );
        Todo savedTodo = todoRepository.save(newTodo);

        return new TodoSaveResponse(
                savedTodo.getId(),
                savedTodo.getTitle(),
                savedTodo.getContents(),
                weather,
                new UserResponse(user.getId(), user.getEmail())
        );
    }

    public Page<TodoResponse> getTodos(int page, int size, String weather, LocalDateTime startDate, LocalDateTime endDate) {

        Page<Todo> todos = null;

        // weather가 null 이고, date가 둘 다 있을 때
        if (weather == null && startDate != null && endDate != null) {
            todos = todoRepository.findByDate(startDate, endDate, PageRequest.of(page, size));
        }
        // weather가 있고, date가 둘 다 null일 때
        if (weather != null && startDate == null && endDate == null) {
            todos = todoRepository.findByWeather(weather, PageRequest.of(page, size));
        }
        // weather도 있고, date도 둘 다 있을 때
        if (weather != null && startDate != null && endDate != null) {
            todos = todoRepository.findByWeatherAndDate(weather, startDate, endDate, PageRequest.of(page, size));
        }
        // weather도 null, date도 전부 null
        if (weather == null && startDate == null && endDate == null) {
            todos = todoRepository.findAll(PageRequest.of(page, size));
        }

        return todos.map(todo -> new TodoResponse(
                todo.getId(),
                todo.getTitle(),
                todo.getContents(),
                todo.getWeather(),
                new UserResponse(todo.getUser().getId(), todo.getUser().getEmail()),
                todo.getCreatedAt(),
                todo.getModifiedAt()
        ));
    }

    public TodoResponse getTodo(long todoId) {
        Todo todo = todoRepository.findByIdWithUser(todoId)
                .orElseThrow(() -> new InvalidRequestException("Todo not found"));

        User user = todo.getUser();

        return new TodoResponse(
                todo.getId(),
                todo.getTitle(),
                todo.getContents(),
                todo.getWeather(),
                new UserResponse(user.getId(), user.getEmail()),
                todo.getCreatedAt(),
                todo.getModifiedAt()
        );
    }

    public Page<TodoSearchResponse> searchTodos(
            String title, String nickname, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {

        return todoRepository.searchTodos(title, nickname, startDate, endDate, pageable);
    }
    /* QueryDSL에서 BooleanExpression을 활용했으므로, null이 발생하면 자동으로 조건에서 제외시켜주기 때문에,
       서비스코드에 if문으로 분기를 나눠줄 필요가 없음!
       반면, BooleanBuilder는 직접 조건을 추가하고 관리해야되서 코드가 복잡해질 수 있다.*/
}
