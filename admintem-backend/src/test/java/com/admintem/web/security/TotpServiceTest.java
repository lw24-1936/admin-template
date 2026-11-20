package com.admintem.web.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TotpServiceTest {

    private final TotpService totp = new TotpService();

    @Test
    void matchesRfc6238Sha1VectorForSixDigits() {
        // RFC 6238 SHA1 secret "12345678901234567890" at T=59 -> 94287082 (8 digits)
        String secret = "GEZDGNBVGY3TQOJQGEZDGNBVGY3TQOJQ";
        assertTrue(totp.verifyAt(secret, "287082", 59L));
        assertFalse(totp.verifyAt(secret, "94287082", 59L));
        assertFalse(totp.verifyAt(secret, "000000", 59L));
    }

    @Test
    void generatesBase32SecretAndOtpAuthUri() {
        String secret = totp.generateSecret();
        assertEquals(32, secret.length());
        String uri = totp.buildOtpAuthUri(secret, "admin", "admintem");
        assertTrue(uri.startsWith("otpauth://totp/"));
        assertTrue(uri.contains("secret=" + secret));
        assertTrue(uri.contains("digits=6"));
    }
}
