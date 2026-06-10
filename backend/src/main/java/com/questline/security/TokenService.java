package com.questline.security;

import com.questline.domain.User;

/**
 * Issues access tokens for authenticated users. The HS256 implementation is the Phase 0
 * default; the seam lets us move to RS256 (asymmetric keys) later without touching callers.
 */
public interface TokenService {

    String issue(User user);
}
