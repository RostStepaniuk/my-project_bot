package ua.work.dao.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.*;
import java.sql.Blob;

@Getter
@Setter
@Entity
public class UserLessons {
    public UserLessons(){}

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String filePath;
//    @ManyToOne
//    @JoinColumn(name = "user_data_chat_id")
//    private UserData userData;
}
