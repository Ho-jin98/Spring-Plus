package org.example.expert.domain.todo.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.example.expert.domain.comment.entity.Comment;
import org.example.expert.domain.common.entity.Timestamped;
import org.example.expert.domain.manager.entity.Manager;
import org.example.expert.domain.user.entity.User;

import java.util.ArrayList;
import java.util.List;

@Getter
@Entity
@NoArgsConstructor
@Table(name = "todos")
public class Todo extends Timestamped {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String title;
    private String contents;
    private String weather;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "todo", cascade = CascadeType.REMOVE)
    private List<Comment> comments = new ArrayList<>();

    /* CascadeType.PERSIST 를 적용하지 않으면,
     todoRepository.save(todo);
     managerRepository.save(managers);
     이런식으로 따로 저장을 해야되지만 CascadeType.PERSIST를 적용하면
     todoRepository.save(todo); -> 이 때 managers도 같이 저장됨!*/
    @OneToMany(mappedBy = "todo", cascade = CascadeType.PERSIST)
    private List<Manager> managers = new ArrayList<>();

    public Todo(String title, String contents, String weather, User user) {
        this.title = title;
        this.contents = contents;
        this.weather = weather;
        this.user = user;
        this.managers.add(new Manager(user, this));
        /* 생성자에 managers를 추가하고 있어도, CascadeType.PERSIST 옵션이 없으면,
         리스트에 추가만 될 뿐 실제 DB에 저장되진 않음,
         managerRepository.save(managers); 를 따로 호출해서 저장해야함*/
    }
}

/* Cascade 옵션
 PERSIST : 부모 저장 시 자식도 함께 저장
 REMOVE : 부모 삭제 시 자식도 함께 삭제
 MERGE : 부모 병합 시 자식도 함께 병합 (준영속 상태를 영속 상태로)
 DETACH : 부모가 영속성 컨텍스트에서 분리될 때 자식도 함께 분리
 REFRESH : 부모를 DB에서 다시 읽어올 때 자식도 함께 갱신
 ALL : 위의 모든 옵션을 한번에 적용*/
