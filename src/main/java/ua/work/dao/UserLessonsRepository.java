package ua.work.dao;

import org.springframework.data.jpa.repository.JpaRepository;
import ua.work.dao.entity.UserLessons;

public interface UserLessonsRepository extends JpaRepository<UserLessons, Long> {
    UserLessons findByFilePath(String filePath);
}
