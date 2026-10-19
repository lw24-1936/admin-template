package com.admintem.web.config.web;

import com.admintem.web.config.AdmintemProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 静态资源映射：把上传目录暴露为可访问的 URL。
 *
 * <p>为什么用资源映射而不是写一个"读文件"的控制器：</p>
 * <ul>
 *   <li>Spring 的资源处理器自带范围请求、缓存头、Last-Modified，浏览器缓存友好；</li>
 *   <li>自己写控制器容易踩"路径穿越"与"MIME 猜测错误"两个坑；</li>
 *   <li>映射目录来自配置，部署时改一个配置即可换存储位置。</li>
 * </ul>
 *
 * <p>安全说明：`/uploads/**` 只存放头像这类公开图片，已在 SecurityConfig 放行。
 * 不要把这个目录用于存放合同、证件等敏感文件——那种场景应改为鉴权后流式下载接口。</p>
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class UploadResourceConfig implements WebMvcConfigurer {

    private final AdmintemProperties properties;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        AdmintemProperties.Upload cfg = properties.getUpload();
        // 必须以 file: 开头且以 / 结尾，否则 Spring 会把路径当成 classpath 资源
        Path dir = Paths.get(cfg.getAvatarDir()).toAbsolutePath().normalize();
        String location = dir.toUri().toString();
        registry.addResourceHandler(cfg.getUrlPrefix() + "/**")
                .addResourceLocations(location)
                // 头像文件名是随机 UUID、内容不变，可以放心让浏览器缓存一天
                .setCachePeriod(86400);
        log.info("头像静态资源映射: {} -> {}（URL: {}/**）", cfg.getUrlPrefix(), dir, cfg.getUrlPrefix());
    }
}
