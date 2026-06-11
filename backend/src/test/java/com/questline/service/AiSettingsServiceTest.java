package com.questline.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import com.questline.ai.AiProviderSettings;
import com.questline.common.SecretCipher;
import com.questline.domain.User;
import com.questline.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AiSettingsServiceTest {

    private static final UUID USER_ID = UUID.randomUUID();

    @Mock
    UserRepository userRepository;

    private final SecretCipher cipher = new SecretCipher("a-test-crypto-secret-32-bytes-minimum");
    private AiSettingsService service;

    @BeforeEach
    void setUp() {
        service = new AiSettingsService(userRepository, cipher);
    }

    @Test
    void resolveReturnsDecryptedSettingsWhenAKeyIsStored() {
        User user = new User();
        user.setId(USER_ID);
        user.setAiBaseUrl("https://openrouter.ai/api/v1");
        user.setAiModel("openai/gpt-4o-mini");
        user.setAiApiKeyEnc(cipher.encrypt("sk-or-secret"));
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        AiProviderSettings settings = service.resolve(USER_ID);

        assertThat(settings).isNotNull();
        assertThat(settings.baseUrl()).isEqualTo("https://openrouter.ai/api/v1");
        assertThat(settings.model()).isEqualTo("openai/gpt-4o-mini");
        assertThat(settings.apiKey()).isEqualTo("sk-or-secret");
    }

    @Test
    void resolveReturnsNullWhenNoKeyConfigured() {
        User user = new User();
        user.setId(USER_ID);
        when(userRepository.findById(USER_ID)).thenReturn(Optional.of(user));

        assertThat(service.resolve(USER_ID)).isNull();
    }
}
