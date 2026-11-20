package com.admintem.web.controller;

import com.admintem.web.aspect.AuditLog;
import com.admintem.web.aspect.AuditTargetType;
import com.admintem.web.common.Result;
import com.admintem.web.entity.dto.DictPageQuery;
import com.admintem.web.entity.dto.DictSaveRequest;
import com.admintem.web.entity.dto.DictTypeRequest;
import com.admintem.web.entity.dto.IdRequest;
import com.admintem.web.entity.dto.validation.ValidationGroups;
import com.admintem.web.entity.vo.DictVO;
import com.admintem.web.entity.vo.PageResult;
import com.admintem.web.security.SecurityUtils;
import com.admintem.web.service.SysDictService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/sysDict")
@RequiredArgsConstructor
@Tag(name = "08-数据字典", description = "字典项维护与按类型读取")
public class SysDictController {

    private final SysDictService dictService;

    @PostMapping("/paginQuery")
    @PreAuthorize("@rbac.hasPerm('sysDict:sysDict')")
    @Operation(summary = "分页查询字典")
    public Result<PageResult<DictVO>> paginQuery(@Valid @RequestBody DictPageQuery query) {
        return Result.ok(dictService.pageQuery(query));
    }

    @PostMapping("/items")
    @PreAuthorize("@rbac.hasPerm('sysDict:sysDict')")
    @Operation(summary = "按类型读取启用字典项")
    public Result<List<DictVO>> items(@Valid @RequestBody DictTypeRequest request) {
        return Result.ok(dictService.items(request.getDictType()));
    }

    @PostMapping("/add")
    @AuditLog(action = "DICT_ADD", name = "新增字典", target = AuditTargetType.CONFIG)
    @PreAuthorize("@rbac.hasPerm('sysDict:add')")
    @Operation(summary = "新增字典项")
    public Result<Long> add(@Validated(ValidationGroups.Create.class) @RequestBody DictSaveRequest request) {
        return Result.ok(dictService.create(request, SecurityUtils.currentUserName()));
    }

    @PostMapping("/edit")
    @AuditLog(action = "DICT_EDIT", name = "编辑字典", target = AuditTargetType.CONFIG)
    @PreAuthorize("@rbac.hasPerm('sysDict:edit')")
    @Operation(summary = "编辑字典项")
    public Result<Void> edit(@Validated(ValidationGroups.Update.class) @RequestBody DictSaveRequest request) {
        dictService.update(request, SecurityUtils.currentUserName());
        return Result.ok();
    }

    @PostMapping("/deleteById")
    @AuditLog(action = "DICT_DELETE", name = "删除字典", target = AuditTargetType.CONFIG)
    @PreAuthorize("@rbac.hasPerm('sysDict:delete')")
    @Operation(summary = "删除字典项")
    public Result<Void> deleteById(@Valid @RequestBody IdRequest request) {
        dictService.delete(request.getId(), SecurityUtils.currentUserName());
        return Result.ok();
    }
}
