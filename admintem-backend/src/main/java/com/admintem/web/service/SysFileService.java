package com.admintem.web.service;

import com.admintem.web.entity.dto.FilePageQuery;
import com.admintem.web.entity.vo.FileVO;
import com.admintem.web.entity.vo.PageResult;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件中心服务。
 */
public interface SysFileService {

    PageResult<FileVO> pageQuery(FilePageQuery query);

    FileVO upload(MultipartFile file, String remark, String operator);

    void delete(Long id, String operator);
}
