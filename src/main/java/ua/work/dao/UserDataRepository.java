package ua.work.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import ua.work.dao.entity.UserData;

public interface UserDataRepository extends JpaRepository<UserData, Long> {
    UserData findByChatIdAndAccessGranted(Long chatId, boolean accessGranted);

}

