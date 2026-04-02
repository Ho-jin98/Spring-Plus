package org.example.expert.domain.manager.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Getter
@Entity
@Table(name = "log")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)

public class Log {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    // 로그 생성 시간
    LocalDateTime createdAt;

    // 어떤 요청인지에 대한 메세지
    private String message;

    /* 성공 / 실패 여부
       true -> success, false -> fail */
    private boolean isSuccess;

    public Log(String message, boolean isSuccess) {
        this.message = message;
        this.isSuccess = isSuccess;
    }
}
