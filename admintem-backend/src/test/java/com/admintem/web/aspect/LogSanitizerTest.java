package com.admintem.web.aspect;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogSanitizerTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Test
    void masksPasswordAndTokenButKeepsStatusCode() {
        String json = "{\"userName\":\"admin\",\"password\":\"12345678\",\"token\":\"abc.def\",\"code\":200}";
        String masked = LogSanitizer.mask(json);
        assertFalse(masked.contains("12345678"));
        assertFalse(masked.contains("abc.def"));
        assertTrue(masked.contains("\"password\":\"***\""));
        assertTrue(masked.contains("\"token\":\"***\""));
        assertTrue(masked.contains("\"code\":200"));
    }

    @Test
    void masksPlainTextAndBearerToken() {
        String text = "LoginRequest(password=secret123, token=abc.def) Authorization=Bearer eyJhbGciOiJIUz";
        String masked = LogSanitizer.mask(text);
        assertFalse(masked.contains("secret123"));
        assertFalse(masked.contains("abc.def"));
        assertFalse(masked.contains("eyJhbGciOiJIUz"));
        assertTrue(masked.contains("password=***"));
        assertTrue(masked.contains("Bearer ***"));
    }

    @Test
    void truncatesLongText() {
        String text = "x".repeat(200);
        String truncated = LogSanitizer.truncate(text, 80);
        assertTrue(truncated.length() < text.length());
        assertTrue(truncated.startsWith("x".repeat(80)));
        assertTrue(truncated.contains("(200 chars)"));
    }

    @Test
    void describesMultipartFileWithoutContent() {
        MockMultipartFile file = new MockMultipartFile("file", "a.png", "image/png", new byte[]{1, 2, 3});
        String text = LogSanitizer.describe(file, mapper, 500);
        assertTrue(text.contains("a.png"));
        assertTrue(text.contains("3B"));
    }

    @Test
    void serializesAndMasksNestedObject() {
        record Login(String userName, String password) {
        }
        String text = LogSanitizer.describe(new Login("admin", "secret"), mapper, 500);
        assertTrue(text.contains("\"userName\":\"admin\""));
        assertFalse(text.contains("secret"));
    }

    @Test
    void limitsMaxLengthToAvoidHugeLogs() {
        record Big(String value) {
        }
        String text = LogSanitizer.describe(new Big("y".repeat(5000)), mapper, 100);
        assertTrue(text.length() > 100);
        assertTrue(text.endsWith(" chars)"));
        assertTrue(text.contains("y".repeat(50)));
    }
}
