package org.example.expert.domain.todo.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.expert.domain.todo.entity.Todo;

import java.util.Optional;

import static org.example.expert.domain.todo.entity.QTodo.todo;

@RequiredArgsConstructor
public class TodoCustomRepositoryImpl implements TodoCustomRepository {

    private final JPAQueryFactory jpaQueryFactory;


    @Override
    public Optional<Todo> findByIdWithUser(Long todoId) {
        return Optional.ofNullable(
                jpaQueryFactory
                        .selectFrom(todo)
                        .leftJoin(todo.user).fetchJoin() //fetchJoin 적용
                        .where(todo.id.eq(todoId))
                        .fetchOne()
        );
    }
    /* Optional.ofNullable -> null일 경우 Optional.empty()를 반환해줌
     -> NPE 방지 , 결과가 없을 수도 있으니, 단전 조회시 Optional.ofNullable를 사용해주는게 안전하다!*/
}
