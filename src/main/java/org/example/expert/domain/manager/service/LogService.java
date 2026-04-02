package org.example.expert.domain.manager.service;

import lombok.RequiredArgsConstructor;
import org.example.expert.domain.manager.entity.Log;
import org.example.expert.domain.manager.repository.LogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LogService {

    private final LogRepository logRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void savedLog(String message, boolean isSuccess) {
        Log log = new Log(message, isSuccess);
        logRepository.save(log);
    }

    /* propagation의 옵션들

     1. REQUIRED : 기본값, 기존 트랜잭션 있으면 참여, 없으면 새로 생성
     2. REQUIRES_NEW : 항상 새로운 트랜잭션 생성, 기존 트랜잭션 일시 중단 => 트랜잭션 분리
     3. SUPPORTS : 트랜잭션 있으면 참여, 없으면 트랜잭션 없이 실행
     4. NOT_SUPPORTED : 트랜잭션 없이 실행, 기존 트랜잭션 일시 중단
     5. MANDATORY : 반드시 기존 트랜잭션 있어야 함, 없으면 예외 발생
     6. NEVER : 트랜잭션 없이 실행, 기존 트랜잭션 있으면 예외 발생
     7. NESTED : 중첩 트랜잭션 생성

     또다른 옵션 noRollbackFor
     ex) @Transactional(noRollbackFor = IllegalArgumentException.class) -> IllegalArgumentException이 발생해도 롤백 안 됨
     기본적으로 @Transactional은 런타임 시 예외가 발생하면 롤백을 하는데, noRollbackFor을 걸어놓으면
     특정 예외는 롤백에서 제외시킬 수 있다.

     propagation 과 noRollbackFor 을 병행하여 같이 사용 하기도 함
     ex) @Transactional(
                propagation = Propagation.REQUIRES_NEW,
                noRollbackFor = IllegalArgumentException.class)
     => 항상 새로운 트랜잭션을 생성하고(트랜잭션 분리), 기존 트랜잭션을 일시 중단 하되,
        IllegalArgumentException이 발생했을 때 롤백에서 제외시켜라

     */

}
