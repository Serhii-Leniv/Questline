package com.questline.service;

import com.questline.common.ApiException;
import com.questline.common.NotFoundException;
import com.questline.domain.User;
import com.questline.repository.UserRepository;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    /**
     * Finds the user by email, creating one on first login. Updates name/image from the
     * identity provider on each login.
     */
    @Transactional
    public User provisionFromOAuth(String email, String name, String image) {
        User user = userRepository.findByEmail(email).orElseGet(User::new);
        user.setEmail(email);
        user.setName(name);
        user.setImage(image);
        return userRepository.save(user);
    }

    @Transactional(readOnly = true)
    public User getById(UUID id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    /** Partial settings update; only non-null fields are applied. */
    @Transactional
    public User updateSettings(UUID userId, String timezone, Integer dailyCapacityMinutes,
                               Integer dailyTaskGoal) {
        User user = getById(userId);
        if (timezone != null) {
            user.setTimezone(validateTimezone(timezone));
        }
        if (dailyCapacityMinutes != null) {
            user.setDailyCapacityMinutes(dailyCapacityMinutes);
        }
        if (dailyTaskGoal != null) {
            user.setDailyTaskGoal(dailyTaskGoal);
        }
        return user;
    }

    /** Deletes the user and all their data (FK cascades handle the rest; see V9 migration). */
    @Transactional
    public void deleteAccount(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new NotFoundException("User not found");
        }
        userRepository.deleteById(userId);
    }

    private static String validateTimezone(String timezone) {
        try {
            return ZoneId.of(timezone).getId();
        } catch (DateTimeException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_TIMEZONE",
                    "Unknown IANA timezone: " + timezone);
        }
    }
}
