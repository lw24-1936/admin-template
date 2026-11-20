package com.admintem.web.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.admintem.web.common.ResultCode;
import com.admintem.web.config.AdmintemProperties;
import com.admintem.web.entity.SysFile;
import com.admintem.web.entity.dto.FilePageQuery;
import com.admintem.web.entity.vo.FileVO;
import com.admintem.web.entity.vo.PageResult;
import com.admintem.web.exception.BusinessException;
import com.admintem.web.mapper.SysFileMapper;
import com.admintem.web.service.FileStorageService;
import com.admintem.web.service.SysFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SysFileServiceImpl implements SysFileService {

    private final SysFileMapper fileMapper;
    private final FileStorageService fileStorageService;
    private final AdmintemProperties properties;

    @Override
    public PageResult<FileVO> pageQuery(FilePageQuery query) {
        long pageNum = query.getPageNum() == null ? 1L : query.getPageNum();
        long pageSize = query.getPageSize() == null ? 10L : query.getPageSize();
        IPage<SysFile> page = fileMapper.selectFilePage(new Page<>(pageNum, pageSize), query);
        List<FileVO> list = page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    @Transactional
    public FileVO upload(MultipartFile file, String remark, String operator) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请选择要上传的文件");
        }
        AdmintemProperties.Upload cfg = properties.getUpload();
        long maxBytes = cfg.getFileMaxSizeMb() * 1024L * 1024L;
        if (file.getSize() > maxBytes) {
            throw new BusinessException(ResultCode.PARAM_ERROR,
                    "文件大小超过限制（最大 " + cfg.getFileMaxSizeMb() + "MB）");
        }
        String originalName = StringUtils.cleanPath(
                file.getOriginalFilename() == null ? "file" : file.getOriginalFilename());
        int dot = originalName.lastIndexOf('.');
        if (dot < 0 || dot == originalName.length() - 1) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "文件缺少扩展名");
        }
        String extension = originalName.substring(dot + 1).toLowerCase(Locale.ROOT);
        Set<String> allowed = Set.of(cfg.getFileAllowedExtensions().split(",")).stream()
                .map(s -> s.trim().toLowerCase(Locale.ROOT))
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toSet());
        if (!allowed.contains(extension)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "不支持的文件类型: " + extension);
        }
        String contentType = StringUtils.hasText(file.getContentType())
                ? file.getContentType() : "application/octet-stream";
        if (contentType.length() > 128) {
            contentType = contentType.substring(0, 128);
        }
        String url = fileStorageService.storeFile(file, extension, contentType);
        SysFile record = new SysFile();
        record.setOriginalName(originalName.length() > 255 ? originalName.substring(0, 255) : originalName);
        record.setUrl(url);
        record.setFileSize(file.getSize());
        record.setContentType(contentType);
        String storageType = properties.getStorage().getType();
        record.setStorageType(StringUtils.hasText(storageType) ? storageType : "local");
        record.setRemark(remark);
        record.setCreatedBy(operator);
        record.setCreatedTime(LocalDateTime.now());
        fileMapper.insert(record);
        log.info("文件上传成功: id={}, name={}, size={}, storage={}",
                record.getId(), originalName, file.getSize(), record.getStorageType());
        return toVO(record);
    }

    @Override
    @Transactional
    public void delete(Long id, String operator) {
        SysFile file = fileMapper.selectById(id);
        if (file == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "文件不存在");
        }
        fileMapper.logicDelete(id, operator, LocalDateTime.now());
        // 先逻辑删除；物理删除失败只记日志，保证元数据状态可控
        try {
            fileStorageService.deleteByUrl(file.getUrl());
        } catch (Exception e) {
            log.warn("文件物理删除失败: id={}, url={}, err={}", id, file.getUrl(), e.getMessage());
        }
    }

    private FileVO toVO(SysFile file) {
        FileVO vo = new FileVO();
        vo.setId(file.getId());
        vo.setOriginalName(file.getOriginalName());
        vo.setUrl(file.getUrl());
        vo.setFileSize(file.getFileSize());
        vo.setContentType(file.getContentType());
        vo.setStorageType(file.getStorageType());
        vo.setRemark(file.getRemark());
        vo.setCreatedBy(file.getCreatedBy());
        vo.setCreatedTime(file.getCreatedTime());
        return vo;
    }
}
