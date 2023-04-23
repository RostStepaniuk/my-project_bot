package ua.work.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import ua.work.dao.UserLessonsRepository;
import ua.work.dao.entity.UserLessons;

import java.util.List;

@Service
public class AudioService {
    @Autowired
    private UserLessonsRepository userLessonsRepository;

    public void saveAudio(String title, String filePath) {
        UserLessons userLessons = new UserLessons();
        userLessons.setTitle(title);
        userLessons.setFilePath(filePath);
        userLessonsRepository.save(userLessons);
    }

    public List<UserLessons> getAllLessons() {
        return userLessonsRepository.findAll();
    }
}
