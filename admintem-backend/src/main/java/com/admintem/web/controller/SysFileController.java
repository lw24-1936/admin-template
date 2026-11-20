package com.admintem.web.controller;

import com.admintem.web.aspect.AuditLog;
import com.admintem.web.aspect.AuditTargetType;
import com.admintem.web.common.Result;
import com.admintem.web.entity.dto.FilePageQuery;
import com.admintem.web.entity.dto.IdRequest;
import com.admintem.web.entity.vo.FileVO;
import com.admintem.web.entity.vo.PageResult;
import com.admintem.web.security.SecurityUtils;
import com.admintem.web.service.SysFileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/sysFile")
@RequiredArgsConstructor
@Tag(name = "09-文件中心", description = "通用附件上传、列表与逻辑删除")
public class SysFileController {

    private final SysFileService fileService;

    @PostMapping("/paginQuery")
    @PreAuthorize("@rbac.hasPerm('sysFile:sysFile')")
    @Operation(summary = "分页查询文件")
    public Result<PageResult<FileVO>> paginQuery(@Valid @RequestBody FilePageQuery query) {
        return Result.ok(fileService.pageQuery(query));
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @AuditLog(action = "FILE_UPLOAD", name = "上传文件", target = AuditTargetType.FILE)
    @PreAuthorize("@rbac.hasPerm('sysFile:upload')")
    @Operation(summary = "上传文件")
    public Result<FileVO> upload(@RequestParam("file") MultipartFile file,
                                 @RequestParam(value = "remark", required = false) String remark) {
        return Result.ok(fileService.upload(file, remark, SecurityUtils.currentUserName()));
    }

    @PostMapping("/deleteById")
    @AuditLog(action = "FILE_DELETE", name = "删除文件", target = AuditTargetType.FILE)
    @PreAuthorize("@rbac.hasPerm('sysFile:delete')")
    @Operation(summary = "删除文件")
    public Result<Void> deleteById(@Valid @RequestBody IdRequest request) {
        fileService.delete(request.getId(), SecurityUtils.currentUserName());
        return Result.ok();
    }
}
