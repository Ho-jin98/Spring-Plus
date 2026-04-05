package org.example.expert.domain.user.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@SpringBootTest
class UserTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    // 배치사이즈 : 1000건씩 묶어서 DB로 전송
    private static final int BATCH_SIZE = 1000;
    // 총 500만건 생성
    private static final int TOTAL = 5_000_000;

    @Test
    void 유저_데이터_500만건_생성_벌크인서트() {
        String sql = "INSERT INTO users (email, password, nickname, user_role, created_at, modified_at) " +
        "VALUES (?, ?, ?, ?, NOW(), NOW())";

        List<Object[]> batch = new ArrayList<>((BATCH_SIZE));

        for (int i = 0; i < TOTAL; i++) {
            String email = "user_" + i + "@test.com";
            String password = "password123";
            String nickname = "nickname_" + i + "_" + UUID.randomUUID().toString().substring(0, 8);
            String userRole = "USER";

            batch.add(new Object[]{email, password, nickname, userRole});

            if (batch.size() == BATCH_SIZE) {
                jdbcTemplate.batchUpdate(sql, batch);
                batch.clear();
            }
        }

        if (!batch.isEmpty()) {
            jdbcTemplate.batchUpdate(sql, batch);
        }
    }

}
/*
 batchUpdate() -> jdbcTemplate에서 제공하는 메서드
 jdbcTemplate.batchUpdate(sql, batch); -> sql : 실행할 INSERT쿼리, batch : 한 번에 보낼 데이터 묶음 (List<Object[]>)
 데이터 묶음을 한번에 DB로 보내달라는 요청, yml파일에 rewriteBatchedStatements=true 설정이 있어야 적용 가능
 */
