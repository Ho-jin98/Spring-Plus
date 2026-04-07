# 🌱 Spring Plus

## 📋 목차

- [기술 스택](#기술-스택)
- [Level 1](#level-1)
    - [Step 1. @Transactional의 이해](#step-1-transactional의-이해)
    - [Step 2. JWT의 이해](#step-2-jwt의-이해)
    - [Step 3. JPA의 이해](#step-3-jpa의-이해)
    - [Step 4. 컨트롤러 테스트의 이해](#step-4-컨트롤러-테스트의-이해)
    - [Step 5. AOP의 이해](#step-5-aop의-이해)
- [Level 2](#level-2)
    - [Step 6. JPA Cascade](#step-6-jpa-cascade)
    - [Step 7. N+1 문제 해결](#step-7-n1-문제-해결)
    - [Step 8. QueryDSL](#step-8-querydsl)
    - [Step 9. Spring Security](#step-9-spring-security)
- [Level 3](#level-3)
    - [Step 10. QueryDSL 검색 기능](#step-10-querydsl-검색-기능)
    - [Step 11. Transaction 심화](#step-11-transaction-심화)
    - [Step 13. 대용량 데이터 처리](#step-13-대용량-데이터-처리)

---

## 기술 스택

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.x-green)
![MySQL](https://img.shields.io/badge/MySQL-8.0-blue)
![Redis](https://img.shields.io/badge/Redis-7.x-red)
![QueryDSL](https://img.shields.io/badge/QueryDSL-5.0-purple)
![Spring Security](https://img.shields.io/badge/Spring_Security-6.x-darkgreen)

---

## Level 1

### Step 1. @Transactional의 이해

#### 문제 상황

`POST /todos` API 호출 시 아래 에러 발생:

```
Connection is read-only. Queries leading to data modification are not allowed
```

#### 원인 분석

`TodoService` 클래스 레벨에 `@Transactional(readOnly = true)` 가 적용되어 있어, 쓰기 작업인 `saveTodo()` 메서드도 읽기 전용으로 실행되는 문제.

| 설정 | 의미 |
|------|------|
| `@Transactional(readOnly = true)` | DB 커넥션을 읽기 전용으로 설정, JPA 더티체킹 비활성화 → SELECT 최적화 |
| `@Transactional` | 쓰기 가능 트랜잭션 → INSERT / UPDATE / DELETE 허용 |

#### 해결

`saveTodo()` 메서드 레벨에 `@Transactional` 을 별도로 선언하여 쓰기 트랜잭션으로 전환.

| 구분 | 결과 |
|------|------|
| 수정 전 | 500 에러 발생 |
| 수정 후 | 정상 저장 |

><div align="center">
>
>**📸 수정 전 / 수정 후 Postman 비교**
>
>| 수정 전 | 수정 후 |
>|---------|---------|
>| <img src="./images/todos%20Postman%20오류%20테스트.png" width="400"/> | <img src="./images/todos%20수정%20후%20postman.png" width="400"/> |
>
></div>

---

### Step 2. JWT의 이해

#### 요구 사항

- `User` 테이블에 `nickname` 컬럼 추가 (중복 가능)
- 프론트엔드에서 JWT 토큰을 파싱해 닉네임을 화면에 표시

#### JWT 구조

| 파트 | 역할 | 암호화 여부 |
|------|------|-------------|
| Header | 토큰 메타데이터 (알고리즘, 타입) | ❌ Base64Url 인코딩만 |
| Payload | 실제 전달 데이터 (Claims) | ❌ Base64Url 인코딩만 |
| Signature | 위변조 방지 서명 | ✅ Secret Key로 서명 |

> ⚠️ Payload는 누구나 디코딩 가능 — 비밀번호, 카드번호 등 민감 정보는 절대 담으면 안 됨

#### 구현 흐름

1. `User` 엔티티에 `nickname` 필드 추가
2. `SignupRequest` DTO에 `nickname` 추가
3. `JwtUtil.createToken()` 파라미터에 `nickname` 추가 → Payload의 Claims에 포함
4. `AuthService` 회원가입/로그인 로직에서 `nickname` 값 전달

#### Q. 로그인용 생성자에 nickname을 추가하지 않았는데 왜 정상 동작할까?

`findByEmail()` 로 DB에서 `User` 전체를 조회하기 때문에 `nickname` 포함 모든 필드가 이미 담겨있어 문제없음.

> <div align="center">
>
> <h4>📸Jwt Payload nickname값 확인</h4>
> 
> <img src="./images/jwt%20페이로드%20nickname.png" width="400"/>
> </div>
> 

---

### Step 3. JPA의 이해

#### 요구 사항

- `weather` 조건으로 할 일 검색 (선택)
- 수정일 기준 기간 검색 (`startDate`, `endDate`) (선택)
- JPQL 사용

#### 쿼리 분기 전략

| weather | startDate & endDate | 사용 쿼리 |
|---------|---------------------|-----------|
| ❌ | ❌ | `findAll()` |
| ✅ | ❌ | `findByWeather()` |
| ❌ | ✅ | `findByDate()` |
| ✅ | ✅ | `findByWeatherAndDate()` |

#### JPQL의 한계

조건이 n개면 쿼리 메서드가 **2ⁿ개** 로 증가.

> 조건 5개 → 32개 / 조건 8개 → 256개

→ 이 한계를 해결하기 위해 이후 단계에서 **QueryDSL + BooleanExpression** 을 활용.

---

### Step 4. 컨트롤러 테스트의 이해

#### 문제 상황

`todo_단건_조회_시_todo가_존재하지_않아_예외가_발생한다()` 테스트 실패.

```
기대: 400 Bad Request
실제: 200 OK
```

#### 원인 분석

`GlobalExceptionHandler` 는 `InvalidRequestException` 발생 시 `400 Bad Request` 를 반환하도록 설정되어 있는데,
테스트 코드에서 `status().isOk()` (200) 를 기대하고 있어서 불일치 발생.

#### 해결

```java
// 수정 전
.andExpect(status().isOk())
.andExpect(jsonPath("$.status").value(HttpStatus.OK.name()))

// 수정 후
.andExpect(status().isBadRequest())
.andExpect(jsonPath("$.status").value(HttpStatus.BAD_REQUEST.name()))
```

---

### Step 5. AOP의 이해

#### 요구 사항

`UserAdminController.changeUserRole()` 메서드 실행 **전** 에 로그가 남아야 함.

#### 원인 분석

`AdminAccessLoggingAspect` 에 `@After` 가 적용되어 메서드 실행 **후** 에 동작하고 있었음.

#### 주요 AOP 어노테이션

| 어노테이션 | 동작 시점 |
|------------|-----------|
| `@Before` | 메서드 실행 전 |
| `@After` | 메서드 실행 후 (성공/실패 무관) |
| `@AfterReturning` | 메서드 정상 반환 후 |
| `@AfterThrowing` | 메서드 예외 발생 후 |
| `@Around` | 실행 전/후 모두 제어 |

#### 해결

`@After` → `@Before` 로 변경.


> <div align="center">
>
>**📸 수정 전 / 수정 후 Postman 비교**
>
>| 수정 전                                                        | 수정 후                                                     |
>|-------------------------------------------------------------|----------------------------------------------------------|
>| <img src="./images/AOP%20After%20상태일%20때.png" width="400"/> | <img src="./images/AOP%20Before로%20변경.png" width="400"/> |
>
> </div>

---

## Level 2

### Step 6. JPA Cascade

#### 요구 사항

할 일 저장 시, 생성한 유저가 담당자로 자동 등록되어야 함.

#### 원인 분석

`Todo` 엔티티 생성자에서 `managers.add(new Manager(user, this))` 로 리스트에는 추가되지만,
`CascadeType.PERSIST` 없이는 실제 DB에 저장되지 않음.

#### Cascade 옵션

| 옵션 | 설명 |
|------|------|
| `PERSIST` | 부모 저장 시 자식도 함께 저장 |
| `REMOVE` | 부모 삭제 시 자식도 함께 삭제 |
| `ALL` | 위 모든 옵션 적용 |

#### 해결

```java
@OneToMany(mappedBy = "todo", cascade = CascadeType.PERSIST)
private List<Manager> managers = new ArrayList<>();
```

---

### Step 7. N+1 문제 해결

#### 문제 상황

`getComments()` API 호출 시 댓글 3개 조회에 쿼리 4번 발생 (1 + N).

```
Comment 3개 조회 → 쿼리 1번
Comment1의 User 조회 → 쿼리 1번
Comment2의 User 조회 → 쿼리 1번
Comment3의 User 조회 → 쿼리 1번
= 총 4번 (댓글 100개면 101번!)
```

#### 해결

JPQL에 `FETCH JOIN` 추가 → 연관 엔티티를 한 번의 쿼리로 함께 조회.

```java
@Query("SELECT c FROM Comment c JOIN FETCH c.user WHERE c.todo.id = :todoId")
List<Comment> findByTodoIdWithUser(@Param("todoId") Long todoId);
```

#### N+1 해결 방법 비교

| 방법 | N+1 해결 | 동적 쿼리 | 페이징 안전 |
|------|----------|-----------|-------------|
| `FETCH JOIN` | ✅ 완전 | ❌ | ⚠️ 주의 |
| `@EntityGraph` | ✅ 완전 | ❌ | ⚠️ 주의 |
| `@BatchSize` | 🔺 부분 | ❌ | ✅ |
| `QueryDSL` | ✅ 완전 | ✅ | ✅ |


> <div align="center">
>
>**📸 수정 전 / 수정 후 Postman 비교**
>
>| 수정 전                                                    | 수정 후                                                             |
>|---------------------------------------------------------|------------------------------------------------------------------|
>| <img src="./images/N+1%20수정%20전%20콘솔.png" width="400"/> | <img src="./images/fetch%20join%20적용%20후%20콘솔.png" width="400"/> |
>
> </div>

---

### Step 8. QueryDSL

#### 요구 사항

JPQL로 작성된 `findByIdWithUser` 를 QueryDSL로 변경 (N+1 방지 유지).

#### QueryDSL 구조

```
TodoCustomRepository      ← 인터페이스 (메서드 정의)
TodoCustomRepositoryImpl  ← 구현체 (QueryDSL 쿼리 작성)
TodoRepository            ← JpaRepository + TodoCustomRepository 상속
```

#### 핵심 코드

```java
jpaQueryFactory
    .selectFrom(todo)
    .leftJoin(todo.user).fetchJoin()  // N+1 방지
    .where(todo.id.eq(todoId))
    .fetchOne()
```

> `fetchJoin()` 없으면 User를 LazyLoading → N+1 발생


---

### Step 9. Spring Security

#### 요구 사항

기존 `Filter` + `ArgumentResolver` 방식 → Spring Security로 전환 (JWT 유지).

#### 전환 전/후 역할 비교

| 역할 | 기존 | 변경 후 |
|------|------|---------|
| 토큰 검증 | `JwtFilter (Filter)` | `JwtFilter (OncePerRequestFilter)` |
| 유저 정보 저장 | `request.setAttribute()` | `SecurityContextHolder` |
| 유저 정보 꺼내기 | `@Auth` + `AuthUserArgumentResolver` | `@AuthenticationPrincipal` |
| URL 권한 설정 | `JwtFilter` 내부 if문 | `SecurityConfig` |

#### 전체 흐름

```
요청
  ↓
JwtFilter (토큰 검증 → SecurityContextHolder에 AuthUser 저장)
  ↓
SecurityConfig (URL별 권한 체크)
  ↓
Controller (@AuthenticationPrincipal로 AuthUser 꺼내서 사용)
```

#### 삭제된 파일

| 파일 | 삭제 이유 |
|------|-----------|
| `FilterConfig` | `SecurityConfig.addFilterBefore()` 로 대체 |
| `AuthUserArgumentResolver` | `@AuthenticationPrincipal` 로 대체 |
| `WebConfig` | `AuthUserArgumentResolver` 삭제로 함께 삭제 |

---

## Level 3

### Step 10. QueryDSL 검색 기능

#### 요구 사항

- 제목 부분 일치 검색 (`contains`)
- 생성일 범위 검색, 최신순 정렬
- 담당자 닉네임 부분 일치 검색
- 응답: 제목, 담당자 수, 총 댓글 수
- 페이징 처리

#### BooleanExpression 활용

조건별 메서드를 분리하여 null-safe하게 처리:

```java
// 제목 검색 (부분 일치)
private BooleanExpression titleContains(String title) {
    return title != null ? todo.title.contains(title) : null;
}

// 닉네임 검색 (부분 일치)
private BooleanExpression managerNicknameContains(String nickname) {
    return nickname != null ? todo.managers.any().user.nickname.contains(nickname) : null;
}
```

> `any()` 를 사용하는 이유: `todo.managers` 는 컬렉션이므로 바로 `user` 에 접근 불가. `any()` 로 "컬렉션 중 하나"로 변환 후 접근.

#### QueryDSL 페이징

```java
// 실제 데이터 조회 (offset + limit)
List<TodoSearchResponse> result = jpaQueryFactory
    .select(Projections.constructor(...))
    .offset(pageable.getOffset())
    .limit(pageable.getPageSize())
    .fetch();

// 전체 건수 조회 (페이징 정보 계산용)
long total = Optional.ofNullable(
    jpaQueryFactory.select(todo.count()).from(todo).fetchOne()
).orElse(0L);

return new PageImpl<>(result, pageable, total);
```

> QueryDSL은 쿼리 실행기이므로, `Page<T>` 반환을 위해 `PageImpl` 로 감싸줘야 함.

---

### Step 11. Transaction 심화

#### 요구 사항

매니저 등록 요청 시 항상 로그가 남아야 함 (등록 실패해도 로그는 저장).

#### 핵심: `Propagation.REQUIRES_NEW`

| 옵션 | 설명 |
|------|------|
| `REQUIRED` (기본값) | 기존 트랜잭션이 있으면 참여 |
| `REQUIRES_NEW` | **항상 새 트랜잭션 생성** (기존 트랜잭션 일시 중단) |

```java
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void savedLog(String message, boolean isSuccess) {
    logRepository.save(new Log(message, isSuccess));
}
```

`REQUIRES_NEW` 덕분에 `ManagerService` 에서 예외가 발생해 롤백되더라도, 이미 커밋된 `LogService` 트랜잭션에는 영향 없음.

#### try-catch 에서 반드시 `throw e` 를 다시 던지는 이유

예외를 잡고 로그를 남긴 뒤, `throw e` 를 하지 않으면 Spring이 에러가 해결된 것으로 착각해 메인 트랜잭션을 롤백하지 않고 커밋할 수 있음.


> <div align="center">
>
> <h4>📸log테이블 매니저 등록 실패 기록</h4>
>
> <img src="./images/Log%20테스트.png" width="400"/>
> </div>

---

### Step 13. 대용량 데이터 처리

#### 요구 사항

- JDBC Bulk Insert로 유저 500만 건 생성
- 닉네임은 랜덤, 중복 최소화
- 닉네임 정확 일치 검색 API
- JPA → 인덱스 → Redis 순으로 성능 개선

---

#### 1. Bulk Insert

##### 왜 Bulk Insert가 필요한가?

| 방식 | INSERT 횟수 | 예상 시간 |
|------|-------------|-----------|
| JPA `save()` 반복 | 500만 번 | 몇 시간 이상 |
| JDBC `batchUpdate()` | 5,000번 (1,000건씩) | 수십 분 이내 |

##### 랜덤 닉네임 & 중복 방지 전략

```java
String nickname = "nickname_" + i + "_" + UUID.randomUUID().toString().substring(0, 8);
```

- `i` (순번) : 앞부분에서 절대 중복 차단
- `UUID 8자리` : 혹시 모를 충돌 방어

##### `rewriteBatchedStatements=true` 가 반드시 필요한 이유

이 옵션 없이 `batchUpdate()` 를 호출하면, 드라이버 내부에서 INSERT를 1건씩 따로 실행함. 이 옵션을 켜야 드라이버가 쿼리를 하나의 거대한 문자열로 합쳐서 한 번에 전송.

```yaml
# application-secret.yml
jdbc:mysql://localhost:3306/spring_plus?rewriteBatchedStatements=true
```

> <div align="center">
>
> <h4>📸User500만 건 데이터 생성</h4>
>
> <img src="./images/User500만건%20데이터%20생성.png" width="400"/>
> </div>

---

#### 2. 성능 개선 결과 비교

| 단계 | 방법 | 조회 속도 |
|------|------|-------|
| 1단계 | JPA 기본 조회 (인덱스 없음) | 7.80s |
| 2단계 | DB 인덱스 추가 (`nickname` 컬럼) | 404ms |
| 3단계 - Cache Miss | Redis 캐시 (최초 조회, DB 조회 후 캐시 저장) | 706ms |
| 3단계 - Cache Hit | Redis 캐시 (재조회, 캐시에서 바로 반환) | 13ms  |

> 
> <div align="center">
>
>**📸 JPA만 사용한 검색결과**
>
> <img src="./images/JPA%20nickname%20검색%20결과%20postman.png" width="400"/>
>
> </div>
>

---

#### 3. 인덱스 추가

```sql
CREATE INDEX idx_nickname ON users (nickname);
```

##### Full Table Scan vs 인덱스 조회

| 방식 | 설명 | 속도 |
|------|------|------|
| Full Table Scan | 500만 건 처음부터 끝까지 전체 스캔 | 느림 |
| Index (B-Tree Ref) | 닉네임으로 바로 탐색, 불필요한 행 접근 최소화 | 빠름 |

> `EXPLAIN` 으로 옵티마이저 전략 확인 시 인덱스 추가 후 `ref` + `idx_nickname` 사용 확인.

>
> <div align="center">
>
> 📸 **EXPLAIN 결과 / 인덱스 추가 전/후 Postman 속도 비교**
>
>| 쿼리 콘솔 - 인덱스 추가 | users 테이블 - 인덱스 적용 확인 |
>|---|---|
>| <img src="./images/user테이블에%20index%20추가%20(쿼리콘솔).png" width="400"/> | <img src="./images/users%20테이블에%20index추가%20(테이블).png" width="400"/> |
>
>| EXPLAIN - 옵티마이저 전략 확인                                                     | 인덱스 추가 후 Postman 조회 속도 |
>|---------------------------------------------------------------------------|---|
>| <img src="./images/EXPLAIN%20INDEX%20(옵티마이저%20전략%20확인).png" width="400"/> | <img src="./images/users테이블에%20index%20추가%20후%20postman.png" width="400"/> |
>
> </div>


---

#### 4. Redis 캐시

##### Redis 란?

Remote Dictionary Server — 데이터를 **메모리(RAM)** 에 저장하는 초고속 Key-Value 데이터베이스.

| 목적 | 사용 방식 |
|------|-----------|
| 데이터의 영구 저장 | MySQL 등 RDBMS |
| 데이터의 빠른 임시 접근 | Redis |
| **이상적인 조합** | DB는 신뢰할 수 있는 원본, Redis는 빠른 복사본 |

##### 적용 전략: Cache-Aside 패턴

```
요청
  ↓
Redis 캐시 확인
  ├── Hit → 캐시에서 즉시 반환 (DB 조회 없음)
  └── Miss → DB 조회 → 결과를 캐시에 저장 → 반환
```

##### RedisTemplate 직접 사용 (vs `@Cacheable`)

`@Cacheable` 대신 `RedisTemplate` 을 직접 사용한 이유: `activateDefaultTyping` 설정 시 `List<UserSearchResponse>` 를 저장할 때 첫 번째 요소가 문자열이 아닌 객체로 들어와 `Unexpected token` 에러가 발생했고, `UserSearchResponse` 에 `LocalDateTime` 필드가 없어 타입 정보를 저장할 필요가 없어 해당 설정 제거 후 `RedisTemplate` 직접 관리 방식으로 해결.

> <div align="center">
>
>**📸 Cache Miss / Cache Hit Postman 비교**
>
>| Cache Miss                                                           | Cache Hit                                                           |
>|----------------------------------------------------------------------|---------------------------------------------------------------------|
>| <img src="./images/redis%20cache%20miss%20postman.png" width="400"/> | <img src="./images/redis%20cache%20hit%20postman.png" width="400"/> |
>
> </div>
