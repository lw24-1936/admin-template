package com.admintem.web.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

import com.admintem.web.entity.dto.AssignRoleRequest;
import com.admintem.web.entity.dto.ResetPasswordRequest;
import com.admintem.web.entity.dto.UserPageQuery;
import com.admintem.web.entity.dto.UserSaveRequest;
import com.admintem.web.entity.vo.PageResult;
import com.admintem.web.entity.vo.UserImportResultVO;
import com.admintem.web.entity.vo.UserRoleVO;
import com.admintem.web.entity.vo.UserVO;

/**
 * 用户管理服务。
 */
public interface SysAuthUserService {

    /**
     * 分页查询用户（含角色信息）。
     *
     * @param query 查询条件
     * @return 分页结果
     */
    PageResult<UserVO> pageQuery(UserPageQuery query);

    /** 导出用：按同一查询条件返回最多 5000 行（调用方负责 CSV 编码与响应头）。 */
    List<UserVO> listForExport(UserPageQuery query);

    /** 导入 CSV 用户（逐行处理，返回成功/失败摘要）。 */
    UserImportResultVO importCsv(MultipartFile file, String operator);

    /**
     * 新增用户（可同时分配角色）。
     *
     * @param request  请求体
     * @param operator 操作人账号
     * @return 新用户ID
     */
    Long createUser(UserSaveRequest request, String operator);

    /**
     * 编辑用户基本信息。
     *
     * @param request  请求体（携带角色列表则一并全量覆盖角色）
     * @param operator 操作人账号
     */
    void updateUser(UserSaveRequest request, String operator);

    /**
     * 删除用户（逻辑删除并清理角色关联、注销其令牌）。
     *
     * @param id       用户ID
     * @param operator 操作人账号
     */
    void deleteUser(Long id, String operator);

    /** 批量启用/禁用（禁用会注销令牌并清理权限缓存）。 */
    void batchUpdateStatus(List<Long> ids, Integer status, String operator);

    /** 批量逻辑删除（内置超级管理员不可删除）。 */
    void batchDelete(List<Long> ids, String operator);

    /**
     * 重置用户密码（管理员操作，被重置用户需重新登录）。
     *
     * @param request  请求体
     * @param operator 操作人账号
     */
    void resetPassword(ResetPasswordRequest request, String operator);

    /**
     * 查询用户已分配角色。
     *
     * @param userId 用户ID
     * @return 用户角色视图
     */
    UserRoleVO getUserRoles(Long userId);

    /**
     * 全量覆盖分配用户角色。
     *
     * @param request  请求体
     * @param operator 操作人账号
     */
    void assignRoles(AssignRoleRequest request, String operator);
}
