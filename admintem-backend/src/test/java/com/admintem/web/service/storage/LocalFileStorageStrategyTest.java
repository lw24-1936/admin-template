package com.admintem.web.service.storage;

import com.admintem.web.config.AdmintemProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LocalFileStorageStrategyTest {

    @TempDir
    Path temp;

    private LocalFileStorageStrategy strategy() {
        AdmintemProperties properties = new AdmintemProperties();
        properties.getUpload().setAvatarDir(temp.toString());
        properties.getUpload().setUrlPrefix("/uploads/avatar");
        return new LocalFileStorageStrategy(properties);
    }

    private MockMultipartFile png() {
        byte[] head = new byte[]{(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        return new MockMultipartFile("file", "avatar.png", "image/png", head);
    }

    @Test
    void storesUnderMonthDirectoryAndDeletes() {
        LocalFileStorageStrategy storage = strategy();
        String url = storage.storeAvatar(png());

        assertTrue(url.matches("^/uploads/avatar/\\d{6}/[a-f0-9]{32}\\.png$"), url);
        Path stored = temp.resolve(url.substring("/uploads/avatar/".length()));
        assertTrue(Files.exists(stored));

        storage.deleteByUrl(url);
        assertFalse(Files.exists(stored));
    }

    @Test
    void ignoresForeignAndTraversalUrls() {
        LocalFileStorageStrategy storage = strategy();
        // 不是本策略前缀：忽略
        storage.deleteByUrl("https://cdn.example.com/a.png");
        // 路径穿越：忽略（不能删到头像目录之外）
        storage.deleteByUrl("/uploads/avatar/../../etc/passwd");
        assertTrue(Files.exists(temp));
    }
}
