package com.questline;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** The RLS transaction manager sets app.user_id from the authenticated user, and unsets it otherwise. */
class RlsTransactionManagerTest extends AbstractIntegrationTest {

    @PersistenceContext
    EntityManager em;

    @Autowired
    PlatformTransactionManager txManager;

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void setsGucFromAuthenticatedUser() {
        UUID userId = UUID.randomUUID();
        Jwt jwt = Jwt.withTokenValue("t").header("alg", "none").subject(userId.toString()).build();
        SecurityContextHolder.getContext().setAuthentication(new JwtAuthenticationToken(jwt));

        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            Object value = em.createNativeQuery("select current_setting('app.user_id', true)")
                    .getSingleResult();
            assertThat(value).isEqualTo(userId.toString());
        });
    }

    @Test
    void leavesGucUnsetWhenNoAuthenticatedUser() {
        new TransactionTemplate(txManager).executeWithoutResult(status -> {
            Object value = em.createNativeQuery("select current_setting('app.user_id', true)")
                    .getSingleResult();
            assertThat(value == null || value.toString().isEmpty()).isTrue();
        });
    }
}
