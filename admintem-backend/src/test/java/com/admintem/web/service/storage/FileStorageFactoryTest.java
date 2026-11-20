package com.admintem.web.service.storage;

import com.admintem.web.config.AdmintemProperties;
import org.junit.jupiter.api.Test;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class FileStorageFactoryTest {

    private StorageStrategy fake(String type) {
        return new StorageStrategy() {
            @Override
            public String type() {
                return type;
            }

            @Override
            public String storeAvatar(MultipartFile file) {
                return type + "://stored";
            }

            @Override
            public String storeFile(MultipartFile file, String extension, String contentType) {
                return type + "://file";
            }

            @Override
            public void deleteByUrl(String url) {
                // no-op
            }
        };
    }

    @Test
    void selectsConfiguredStrategy() {
        AdmintemProperties properties = new AdmintemProperties();
        List<StorageStrategy> strategies = List.of(fake("local"), fake("minio"));

        properties.getStorage().setType("local");
        FileStorageFactory local = new FileStorageFactory(strategies, properties);
        assertEquals("local", local.activeType());

        properties.getStorage().setType("minio");
        FileStorageFactory minio = new FileStorageFactory(strategies, properties);
        assertEquals("minio", minio.activeType());
    }

    @Test
    void failsFastOnUnknownType() {
        AdmintemProperties properties = new AdmintemProperties();
        properties.getStorage().setType("oss");
        assertThrows(IllegalStateException.class,
                () -> new FileStorageFactory(List.of(fake("local")), properties));
    }
}
