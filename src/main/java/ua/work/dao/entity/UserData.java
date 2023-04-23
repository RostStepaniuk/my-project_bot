package ua.work.dao.entity;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.sql.Timestamp;

@Getter
@Setter
@Entity(name = "userData")
public class UserData {
    public UserData(){}
    @Id
    private Long chatId;
    private String userName;

    private Timestamp activationDate;

    private boolean accessGranted;

    private Integer numberLesson;

    private Timestamp dispatchTime;

}
