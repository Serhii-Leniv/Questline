package com.questline.security;

import com.questline.domain.User;
import java.time.Instant;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

/**
 * Issues self-signed HS256 JWTs. The token subject is the user id, so the resource server
 * can resolve the authenticated user without trusting any request body.
 */
@Service
public class HmacTokenService implements TokenService {

    private final JwtEncoder jwtEncoder;
    private final AppProperties props;

    public HmacTokenService(JwtEncoder jwtEncoder, AppProperties props) {
        this.jwtEncoder = jwtEncoder;
        this.props = props;
    }

    @Override
    public String issue(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(props.jwt().issuer())
                .issuedAt(now)
                .expiresAt(now.plus(props.jwt().ttl()))
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("name", user.getName())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }
}
