package com.questline.service;

import com.questline.common.NotFoundException;
import com.questline.domain.PlanType;
import com.questline.domain.User;
import com.questline.repository.UserRepository;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Stub billing: flips the plan immediately with no payment. The default implementation so the app
 * runs without Stripe; a real {@code StripeBillingService} can replace it (it would be the primary
 * bean, and this one steps aside via {@link ConditionalOnMissingBean}).
 */
@Service
@ConditionalOnMissingBean(name = "stripeBillingService")
public class MockBillingService implements BillingService {

    private static final Logger log = LoggerFactory.getLogger(MockBillingService.class);

    private final UserRepository userRepository;

    public MockBillingService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public void upgradeToPro(UUID userId) {
        setPlan(userId, PlanType.PRO);
        log.info("[billing-mock] user {} upgraded to PRO (no real payment)", userId);
    }

    @Override
    @Transactional
    public void downgradeToFree(UUID userId) {
        setPlan(userId, PlanType.FREE);
        log.info("[billing-mock] user {} downgraded to FREE", userId);
    }

    private void setPlan(UUID userId, PlanType plan) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("User not found"));
        user.setPlan(plan);
    }
}
