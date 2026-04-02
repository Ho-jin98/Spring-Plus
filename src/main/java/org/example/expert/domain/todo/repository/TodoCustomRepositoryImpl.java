package org.example.expert.domain.todo.repository;

import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.example.expert.domain.todo.dto.response.TodoSearchResponse;
import org.example.expert.domain.todo.entity.Todo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.example.expert.domain.comment.entity.QComment.comment;
import static org.example.expert.domain.manager.entity.QManager.manager;
import static org.example.expert.domain.todo.entity.QTodo.todo;
import static org.example.expert.domain.user.entity.QUser.user;

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

    @Override
    public Page<TodoSearchResponse> searchTodos(
            String title, String nickname, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable) {

        // 1. 실제 검색 결과를 가져오는 쿼리

        List<TodoSearchResponse> result =  jpaQueryFactory
                .select(Projections.constructor(TodoSearchResponse.class,
                                todo.title, todo.managers.size().longValue(), todo.comments.size().longValue()))
                .from(todo)
                .leftJoin(user).on(todo.user.id.eq(user.id))
                .where(
                        titleContains(title),
                        managerNicknameContains(nickname),
                        createdAtContains(startDate, endDate)
                )
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(todo.createdAt.desc())
                .fetch();


        /* 1번 실제 검색 결과를 가져오는 쿼리 내용 정리

        Projections.constructor -> TodoSearchResponse 생성자에 맞게 필드를 매핑

        todo.title -> 제목
        todo.managers.size() -> 담당자 수
        todo.comments.size() -> 댓글 수
        leftJoin(user) -> todo와 user를 조인
        leftJoin(manager) -> todo와 manager를 조인
        where -> 검색 조건 (제목, 닉네임, 생성일 범위)
        offset -> 몇 번째 데이터부터 가져올지 (페이징)
        limit -> 한 페이지에 몇 개를 가져올지 (페이징)
        orderBy(todo.createdAt.desc()) -> 생성일 최신순 정렬
        fetch() -> List<TodoSearchResponse> 로 반환 */


        // 2. 페이징 처리를 위해 전체 데이터 개수 조회
        long total = Optional.ofNullable(jpaQueryFactory
                .select(todo.count())
                .from(todo)
                .fetchOne())
                .orElse(0L);

        /* todo.count() : todo 전체 개수를 COUNT 쿼리로 조회
           fetchOne() -> 단건 조회 (null 가능)
           Optional.ofNullable -> null이면 Optional.empty() 반환해서 NPE 방지
           orElse(0L) -> null이면 0으로 대체 */


        return new PageImpl<>(result, pageable, total);
        /* result -> 실제 검색된 데이터 목록
           pageable -> 페이지 번호, 크기 등 페이징 정보
           total -> 전체 데이터 개수 */

    }

    private BooleanExpression titleContains(String title) {
        return title != null ? todo.title.contains(title) : null;
    }
    /* todo.title.contains(title)
     -> 해당 todo의 제목이 keyword를 포함하고 있으면 검색결과에 포함시킨다*/

    private BooleanExpression managerNicknameContains(String nickname) {
        return nickname != null ? todo.managers.any().user.nickname.contains(nickname) : null;
    }

    /* any() -> 컬렉션에서 하나라도 조건에 맞는 요소가 있으면 true를 반환해주는 메서드
       todo.managers.any().user.nickname.contains(nickname)
       -> 해당 todo의 담당자들 중에서 닉네임에 keyword가 포함된 담당자가 하나라도 있으면 검색결과에 포함시킨다는 의미*/

    /* todo에서 nickname에 접근하려면 todo -> managers -> user -> nickname 으로 접근을 해야하는데,
       todo.managers를 하면 user는 안보이고 다른 목록들이 뜨는데, 그 이유는
       todo의 엔티티를 보면, 양방향 관계로 List<Manager>를 들고 있음,
       여러개의 Manager가 있을 수 있고, 이 중에서 어떤 Manager의 user에게 접근할지 모르기 때문에,
       바로 user에게 접근 할 수 없기 때문임, any()로 "컬렉션 중 하나에 접근하겠다" 변환을 해줘야
       .user.nickname 으로 접근할 수 있다!!*/

    /* Todo에서 검색기능을 구현하는 거니까 Todo엔티티의 연관관계를 살펴보고 결정을 해야한다!
       타 도메인의 엔티티를 보고 결정하는 것이 아니다!*/


    private BooleanExpression createdAtContains(LocalDateTime startDate, LocalDateTime endDate) {
        // startDate와 endDate 둘 다 있을 때
        if (startDate != null && endDate != null) {
            return todo.createdAt.between(startDate, endDate);
            // todo의 생성일이 시작일과 종료일 사이에 있는 것
        }
        // startDate만 있을 때
        if (startDate != null) {
            // todo의 생성일이 시작일 보다 크거나 같은 것
            // created >= startDate -> 시작일만 있을 때는 시작일 이후의 데이터를 조회
            return todo.createdAt.goe(startDate);
        }
        // endDate만 있을 때
        if (endDate != null) {
            // todo의 생성일이 종료일 보다 작거나 같은 것
            // createdAt <= endDate -> 종료일만 있을 때 종료일 이전 데이터를 조회
            return todo.createdAt.loe(endDate);
        }
        return null;

        /* goe, loe는 QueryDSL에서 제공하는 비교 연산자이다
         goe : Greater On Equal : >= 크거나 같다.
         loe : Less Or Equal : <= 작거나 같다.

         gt : GreaterThen : > 초과
         lt : LessThen : < 미만 */
    }
}
/* 1번 실제 검색 결과를 가져오는 로직과, 2번 페이징 처리를 위해 전체 데이터를 조회하는 로직을 합쳐볼 순 없을까? -> X
   result와 total은 각각 독립적인 쿼리로 각자 다른 정보들을 조회해오는 로직들인데,
   result는 제목, 담당자수, 댓글 수를 조회해오고,
   total은 전체 데이터의 갯수를 조회해온다.
   서로 다른 쿼리라서 하나로 합치면 결과가 달라질 위험이 있을 것 같다.*/
