package com.admintem.web.service.storage;

import com.admintem.web.config.AdmintemProperties;
import com.admintem.web.exception.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImageUploadValidatorTest {

    private final AdmintemProperties.Upload cfg = new AdmintemProperties.Upload();

    private MockMultipartFile file(String name, String contentType, byte[] content) {
        return new MockMultipartFile("file", name, contentType, content);
    }

    private byte[] png() {
        return new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 0, 0, 0, 0};
    }

    @Test
    void acceptsRealPng() {
        assertEquals("png", ImageUploadValidator.validate(file("a.png", "image/png", png()), cfg));
    }

    @Test
    void rejectsSpoofedExtension() {
        byte[] text = "not an image".getBytes();
        assertThrows(BusinessException.class,
                () -> ImageUploadValidator.validate(file("a.png", "image/png", text), cfg));
    }

    @Test
    void rejectsUnsupportedExtension() {
        assertThrows(BusinessException.class,
                () -> ImageUploadValidator.validate(file("a.exe", "application/octet-stream", png()), cfg));
    }

    @Test
    void rejectsOversize() {
        cfg.setMaxSizeMb(1);
        byte[] big = new byte[1024 * 1024 + 1];
        big[0] = (byte) 0x89;
        big[1] = 'P';
        big[2] = 'N';
        big[3] = 'G';
        assertThrows(BusinessException.class,
                () -> ImageUploadValidator.validate(file("a.png", "image/png", big), cfg));
    }

    @Test
    void rejectsEmpty() {
        assertThrows(BusinessException.class,
                () -> ImageUploadValidator.validate(file("a.png", "image/png", new byte[0]), cfg));
    }
}
