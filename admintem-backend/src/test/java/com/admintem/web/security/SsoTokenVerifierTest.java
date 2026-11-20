package com.admintem.web.security;

import com.admintem.web.common.ResultCode;
import com.admintem.web.config.AdmintemProperties;
import com.admintem.web.exception.BusinessException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SsoTokenVerifierTest {

    private static final String SECRET = "0123456789abcdef0123456789abcdef";

    private AdmintemProperties properties;
    private SsoTokenVerifier verifier;

    @BeforeEach
    void setUp() {
        properties = new AdmintemProperties();
        properties.getAuth().getSso().setEnabled(true);
        properties.getAuth().getSso().setSecret(SECRET);
        properties.getAuth().getSso().setIssuer("test-issuer");
        properties.getAuth().getSso().setAudience("test-aud");
        verifier = new SsoTokenVerifier(properties);
    }

    @Test
    void verifiesValidAssertion() {
        String token = token("admin", "jti-1", "test-aud");
        Claims claims = verifier.verify(token);
        assertEquals("admin", claims.getSubject());
        assertEquals("jti-1", claims.getId());
    }

    @Test
    void rejectsWrongAudience() {
        String token = token("admin", "jti-2", "other-aud");
        BusinessException error = assertThrows(BusinessException.class, () -> verifier.verify(token));
        assertEquals(ResultCode.UNAUTHORIZED.getCode(), error.getCode());
    }

    private String token(String subject, String jti, String audience) {
        Date now = new Date();
        return Jwts.builder()
                .subject(subject)
                .id(jti)
                .issuer("test-issuer")
                .audience().add(audience).and()
                .issuedAt(now)
                .expiration(new Date(now.getTime() + 60_000))
                .signWith(Keys.hmacShaKeyFor(SECRET.getBytes(StandardCharsets.UTF_8)))
                .compact();
    }
}
