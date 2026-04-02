package org.example.expert.domain.todo.dto.response;

import lombok.Getter;

@Getter

public class TodoSearchResponse {

    // 제목
    private String title;

    // 담당자 인원 수
    private Long countManagers;

    // 총 댓글 갯수
    private Long totalComments;

    public TodoSearchResponse(String title, Long countManagers, Long totalComments) {
        this.title = title;
        this.countManagers = countManagers;
        this.totalComments = totalComments;
    }
}
