package org.example.expert.domain.todo.repository;

import org.example.expert.domain.todo.entity.Todo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface TodoRepository extends JpaRepository<Todo, Long> {

    @Query("SELECT t FROM Todo t LEFT JOIN FETCH t.user u ORDER BY t.modifiedAt DESC")
    Page<Todo> findAllByOrderByModifiedAtDesc(Pageable pageable);

    @Query("SELECT t FROM Todo t " +
            "LEFT JOIN t.user " +
            "WHERE t.id = :todoId")
    Optional<Todo> findByIdWithUser(@Param("todoId") Long todoId);

    // weather와 date 둘 다 없을 때
    Page<Todo> findAll(Pageable pageable);

    // weather만 있을 때
    @Query("SELECT t FROM Todo t JOIN FETCH t.user u WHERE t.weather = :weather ORDER BY t.modifiedAt DESC")
    Page<Todo> findByWeather(@Param("weather") String weather, Pageable pageable);

    // date만 있을 때
    @Query("SELECT t FROM Todo t JOIN FETCH t.user u WHERE t.modifiedAt >:startDate AND t.modifiedAt < :endDate")
    Page<Todo> findByDate(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate, Pageable pageable);

    // weather와 date 둘 다 있을 때
    @Query("""
            SELECT t FROM Todo t JOIN FETCH t.user u WHERE t.weather = :weather AND t.modifiedAt >:startDate
                      AND t.modifiedAt <:endDate ORDER BY t.modifiedAt DESC
          """)
    Page<Todo> findByWeatherAndDate(@Param("weather") String weather, @Param("startDate") LocalDateTime startDate,
                                    @Param("endDate") LocalDateTime endDate, Pageable pageable);
}

/* WHERE t.weather = :weather : Todo의 weather 필드가 파라미터로 넘긴 weather랑 같은 것만 조회
   t.modifiedAt > :startDate : 수정일이 시작일보다 이후인 것만 조회
   t.modifiedAt < :endDate : 수정일이 종료일보다 이전인 것만 조회
   ORDER BY t.modifiedAt DESC : 수정일 기준 최신순 정렬

   예를 들면, 시작일(startDate): 2024-01-01, 종료일(endDate): 2024-01-31
   t.modifiedAt > 2024-01-01 : 1월1일 이후에 수정 된 것
   t.modifiedAt < 2024-01-31 : 1월 31일 이전에 수정된 것
   이 두 조건을 AND로 합치면 1월1일 ~ 1월31일 사이에 수정된 것만 조회됨

   그리고 기준은 "수정일" modifiedAt임, 시작일과 종료일은 단순 날짜라고 생각하면 됨
   수정일 -> 시작일과 종료일 사이에 있는 Todo만 가져오는 것!
   */
