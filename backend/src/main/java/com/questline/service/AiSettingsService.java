package com.questline.service;

import com.questline.ai.AiProviderSettings;
import com.questline.common.FieldCipher;
import com.questline.common.NotFoundException;
import com.questline.domain.User;
import com.questline.repository.UserRepository;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Per-user AI provider settings (BYOK). A user can store their own OpenAI-compatible endpoint +
 * model + API key (encrypted at rest); when set, their AI jobs use it instead of the server
 * default. The key is never returned to the client.
 */
@Service
public class AiSettingsService {

    private final UserRepository userRepository;
    private final FieldCipher cipher;

    public AiSettingsService(UserRepository userRepository, FieldCipher cipher) {
        this.userRepository = userRepository;
        this.cipher = cipher;
    }

    /** The user's BYOK credentials for an LLM call, or null to fall back to the server default. */
    @Transactional(readOnly = true)
    public AiProviderSettings resolve(UUID userId) {
        User user = userRepository.findById(userId).orElse(null);
        if (user == null || isBlank(user.getAiApiKeyEnc())) {
            return null;
        }
        return new AiProviderSettings(user.getAiBaseUrl(), cipher.decrypt(user.getAiApiKeyEnc()),
                user.getAiModel());
    }

    @Transactional
    public void update(UUID userId, String baseUrl, String model, String apiKey) {
        User user = getUser(userId);
        user.setAiBaseUrl(baseUrl);
        user.setAiModel(model);
        // A blank apiKey leaves the stored one untouched (so editing base-url/model needn't resend it).
        if (!isBlank(apiKey)) {
            user.setAiApiKeyEnc(cipher.encrypt(apiKey));
        }
    }

    @Transactional
    public void clear(UUID userId) {
        User user = getUser(userId);
        user.setAiBaseUrl(null);
        user.setAiModel(null);
        user.setAiApiKeyEnc(null);
    }

    @Transactional(readOnly = true)
    public Status status(UUID userId) {
        User user = getUser(userId);
        return new Status(!isBlank(user.getAiApiKeyEnc()), user.getAiBaseUrl(), user.getAiModel());
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    /** Non-secret view of a user's AI settings (never includes the key). */
    public record Status(boolean configured, String baseUrl, String model) {
    }
}
