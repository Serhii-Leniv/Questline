package com.questline.service;

import java.util.UUID;

/**
 * Billing seam. The mock implementation flips the plan immediately; a real Stripe implementation
 * would start a Checkout session and flip the plan on a webhook — callers depend only on this.
 */
public interface BillingService {

    void upgradeToPro(UUID userId);

    void downgradeToFree(UUID userId);
}
