package com.admintem.web.service.impl;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.admintem.web.common.ResultCode;
import com.admintem.web.config.AdmintemProperties;
import com.admintem.web.entity.SysAuthRole;
import com.admintem.web.entity.SysAuthUser;
import com.admintem.web.entity.SysDept;
import com.admintem.web.entity.SysAuthUserRole;
import com.admintem.web.entity.dto.AssignRoleRequest;
import com.admintem.web.entity.dto.ResetPasswordRequest;
import com.admintem.web.entity.dto.UserPageQuery;
import com.admintem.web.entity.dto.UserSaveRequest;
import com.admintem.web.entity.vo.PageResult;
import com.admintem.web.entity.vo.UserImportResultVO;
import com.admintem.web.entity.vo.UserRoleVO;
import com.admintem.web.entity.vo.UserVO;
import com.admintem.web.exception.BusinessException;
import com.admintem.web.mapper.SysAuthRoleMapper;
import com.admintem.web.mapper.SysAuthDeptMapper;
import com.admintem.web.mapper.SysAuthUserMapper;
import com.admintem.web.mapper.SysAuthUserRoleMapper;
import com.admintem.web.security.JwtTokenService;
import com.admintem.web.security.PasswordPolicy;
import com.admintem.web.security.RbacCacheService;
import com.admintem.web.service.SysAuthUserService;
import com.admintem.web.service.UserRbacService;
import com.admintem.web.support.AfterCommitExecutor;
import com.admintem.web.util.query.SortWhitelist;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 用户管理服务实现（数据访问统一走 Mapper XML）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysAuthUserServiceImpl implements SysAuthUserService {

    private final SysAuthUserMapper userMapper;
    private final SysAuthRoleMapper roleMapper;
    private final SysAuthUserRoleMapper userRoleMapper;
    /** 部门（用户列表展示部门名；V1.0.9） */
    private final SysAuthDeptMapper deptMapper;
    private final UserRbacService userRbacService;
    private final RbacCacheService rbacCacheService;
    private final JwtTokenService jwtTokenService;
    private final PasswordEncoder passwordEncoder;
    /** 密码策略：复杂度校验（配置开启时生效） */
    private final PasswordPolicy passwordPolicy;
    private final AdmintemProperties properties;
    /** 缓存失效/令牌吊销统一挂到事务提交之后（见 AfterCommitExecutor 的时序说明） */
    private final AfterCommitExecutor afterCommitExecutor;

    /**
     * 用户列表排序白名单：前端逻辑字段名 → 数据库列名。
     *
     * <p>只有登记在此的字段才能进入 ORDER BY，未登记字段由
     * {@link SortWhitelist#resolve} 返回 null 并回落默认排序（created_time DESC）。</p>
     */
    private static final Map<String, String> SORT_WHITELIST = Map.of(
            "userId", "id",
            "userName", "user_name",
            "nickName", "nick_name",
            "email", "email",
            "phone", "phone",
            "status", "status",
            "createdTime", "created_time");

    /**
     * {@inheritDoc}
     */
    @Override
    public PageResult<UserVO> pageQuery(UserPageQuery query) {
        // 0. 分页参数兜底：@RequestBody 显式传 null 时 Integer 拆箱会抛 NPE（HTTP 500）
        long pageNum = query.getPageNum() == null ? 1L : query.getPageNum();
        long pageSize = query.getPageSize() == null ? 10L : query.getPageSize();
        // 0.1 排序：仅接受白名单内的逻辑字段名，未登记则回落默认排序（不报错、不回显列名）
        String orderBy = SortWhitelist.resolve(query.getSortField(), query.getSortOrder(), SORT_WHITELIST);
        if (StringUtils.hasText(query.getSortField()) && orderBy == null) {
            log.warn("用户列表排序字段不在白名单，已忽略: sortField={}", query.getSortField());
        }
        // 1. 分页查询用户（SQL 在 XML：多条件过滤 + 白名单排序）
        IPage<SysAuthUser> page = userMapper.selectUserPage(new Page<>(pageNum, pageSize), query, orderBy);
        List<SysAuthUser> records = page.getRecords();
        if (CollectionUtils.isEmpty(records)) {
            return new PageResult<>(Collections.emptyList(), page.getTotal(), page.getCurrent(), page.getSize());
        }
        // 2. 批量补充角色信息（一次关联查询 + 一次角色批量查询）
        Map<Long, List<SysAuthRole>> rolesByUser = loadRolesMap(records.stream()
                .map(SysAuthUser::getId).collect(Collectors.toList()));
        // 2.1 批量补部门名（一次查询，避免逐个用户查部门）
        Map<Long, String> deptNameById = loadDeptNames(records);
        // 3. 组装 VO
        List<UserVO> list = records.stream().map(u -> {
            UserVO vo = toVO(u);
            List<SysAuthRole> roles = rolesByUser.getOrDefault(u.getId(), Collections.emptyList());
            vo.setRoleIds(roles.stream().map(SysAuthRole::getId).collect(Collectors.toList()));
            vo.setRoleNames(roles.stream().map(SysAuthRole::getRoleName).collect(Collectors.toList()));
            vo.setDeptName(u.getDeptId() == null ? null : deptNameById.get(u.getDeptId()));
            return vo;
        }).collect(Collectors.toList());
        return new PageResult<>(list, page.getTotal(), page.getCurrent(), page.getSize());
    }

    @Override
    public List<UserVO> listForExport(UserPageQuery query) {
        IPage<SysAuthUser> page = userMapper.selectUserPage(new Page<>(1, 5000), query, null);
        return page.getRecords().stream().map(this::toVO).collect(Collectors.toList());
    }

    @Override
    public UserImportResultVO importCsv(MultipartFile file, String operator) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请选择 CSV 文件");
        }
        if (file.getSize() > 2 * 1024 * 1024) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "CSV 文件不能超过 2MB");
        }
        String text;
        try {
            text = new String(file.getBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new BusinessException(ResultCode.INTERNAL_ERROR, "读取 CSV 失败");
        }
        if (text.startsWith("\uFEFF")) {
            text = text.substring(1);
        }
        String[] lines = text.split("\\r?\\n");
        if (lines.length < 2) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "CSV 缺少数据行");
        }
        Map<String, Integer> header = headerIndex(lines[0]);
        if (!header.containsKey("username") || !header.containsKey("password")) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "CSV 表头必须包含 userName 与 password");
        }
        UserImportResultVO result = new UserImportResultVO();
        for (int i = 1; i < lines.length; i++) {
            if (!StringUtils.hasText(lines[i])) {
                continue;
            }
            result.setTotal(result.getTotal() + 1);
            List<String> row = parseCsvLine(lines[i]);
            try {
                UserSaveRequest request = new UserSaveRequest();
                request.setUserName(value(row, header, "username"));
                request.setPassword(value(row, header, "password"));
                request.setNickName(value(row, header, "nickname"));
                request.setEmail(value(row, header, "email"));
                request.setPhone(value(row, header, "phone"));
                request.setDeptId(longValue(value(row, header, "deptid")));
                request.setStatus(intValue(value(row, header, "status"), 0));
                if (!StringUtils.hasText(request.getPassword())) {
                    throw new BusinessException(ResultCode.PARAM_ERROR, "密码不能为空");
                }
                createUser(request, operator);
                result.setSuccess(result.getSuccess() + 1);
            } catch (Exception e) {
                result.setFailed(result.getFailed() + 1);
                if (result.getErrors().size() < 20) {
                    result.getErrors().add("第 " + (i + 1) + " 行: " + e.getMessage());
                }
            }
        }
        return result;
    }

    private Map<String, Integer> headerIndex(String line) {
        Map<String, Integer> map = new HashMap<>();
        List<String> cells = parseCsvLine(line);
        for (int i = 0; i < cells.size(); i++) {
            map.put(cells.get(i).trim().toLowerCase(), i);
        }
        return map;
    }

    private String value(List<String> row, Map<String, Integer> header, String key) {
        Integer index = header.get(key);
        if (index == null || index >= row.size()) {
            return null;
        }
        String value = row.get(index).trim();
        return value.isEmpty() ? null : value;
    }

    private Long longValue(String value) {
        return StringUtils.hasText(value) ? Long.valueOf(value) : null;
    }

    private Integer intValue(String value, int defaultValue) {
        return StringUtils.hasText(value) ? Integer.valueOf(value) : defaultValue;
    }

    /** 解析带引号的单行 CSV（支持逗号与转义双引号；不跨行）。 */
    private List<String> parseCsvLine(String line) {
        List<String> cells = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean quoted = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                if (quoted && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    quoted = !quoted;
                }
            } else if (c == ',' && !quoted) {
                cells.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        cells.add(current.toString());
        return cells;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createUser(UserSaveRequest request, String operator) {
        // 账号唯一性
        if (userMapper.countByUserName(request.getUserName(), null) > 0) {
            throw new BusinessException(ResultCode.USERNAME_EXISTS);
        }
        if (!StringUtils.hasText(request.getPassword())) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "新增用户必须填写初始密码");
        }
        // 密码复杂度（配置 admintem.auth.password.complexity-enabled 开启时生效）
        passwordPolicy.validateComplexity(request.getPassword());
        SysAuthUser user = new SysAuthUser();
        // 写入登录账号（关键字段，缺失会导致用户无法登录）
        user.setUserName(request.getUserName());
        applyBaseFields(request, user);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        if (user.getStatus() == null) {
            user.setStatus(0);
        }
        user.setCreatedBy(operator);
        user.setCreatedTime(LocalDateTime.now());
        try {
            userMapper.insert(user);
        } catch (DuplicateKeyException e) {
            // 并发兜底：上面的 count 校验与 insert 之间存在窗口，最终由唯一索引
            // uk_user_name_active（V1.0.6）拒绝；此处转换成与串行场景一致的业务提示
            log.warn("并发创建同名账号被数据库唯一索引拒绝: userName={}", request.getUserName());
            throw new BusinessException(ResultCode.USERNAME_EXISTS);
        }
        log.info("新增用户成功: id={}, userName={}, operator={}", user.getId(), user.getUserName(), operator);

        // 可选：一并分配角色
        if (!CollectionUtils.isEmpty(request.getRoleIds())) {
            checkRoleIdsExist(request.getRoleIds());
            replaceUserRoles(user.getId(), request.getRoleIds(), operator);
        }
        return user.getId();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(UserSaveRequest request, String operator) {
        SysAuthUser user = userMapper.selectById(request.getId());
        if (user == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "用户不存在");
        }
        // 账号不可修改（作为登录标识保持稳定）
        if (StringUtils.hasText(request.getUserName()) && !request.getUserName().equals(user.getUserName())) {
            throw new BusinessException(ResultCode.BUILT_IN_DATA, "登录账号不可修改");
        }
        boolean targetIsAdmin = userHasAdminRole(user.getId());

        // 内置管理员（id=1）及拥有 admin 角色的用户不允许被禁用
        if (request.getStatus() != null && request.getStatus() == 1
                && (Long.valueOf(1L).equals(user.getId()) || targetIsAdmin)) {
            throw new BusinessException(ResultCode.BUILT_IN_DATA, "超级管理员账号不可被禁用");
        }
        applyBaseFields(request, user);
        // 传了新密码才修改密码
        if (StringUtils.hasText(request.getPassword())) {
            passwordPolicy.validateComplexity(request.getPassword());
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        user.setUpdateBy(operator);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateSelective(user);
        log.info("编辑用户成功: id={}, operator={}", user.getId(), operator);

        // 角色列表一并全量覆盖
        if (request.getRoleIds() != null) {
            checkRoleIdsExist(request.getRoleIds());
            replaceUserRoles(user.getId(), request.getRoleIds(), operator);
        }
        // 状态/资料变化后清理缓存；若被禁用还需使其令牌立即失效。
        // 两者都必须在**事务提交后**执行，否则并发读会把旧数据回填进缓存（见 AfterCommitExecutor）。
        boolean disabled = user.getStatus() != null && user.getStatus() == 1;
        Long updatedUserId = user.getId();
        String updatedUserName = user.getUserName();
        afterCommitExecutor.run(() -> {
            rbacCacheService.evictUser(updatedUserId);
            if (disabled) {
                jwtTokenService.revokeToken(updatedUserName);
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id, String operator) {
        SysAuthUser user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "用户不存在");
        }
        if (Long.valueOf(1L).equals(id)) {
            throw new BusinessException(ResultCode.BUILT_IN_DATA, "内置管理员账号不可删除");
        }
        if (userHasAdminRole(id)) {
            throw new BusinessException(ResultCode.BUILT_IN_DATA, "拥有超级管理员角色的用户不可删除");
        }
        // 逻辑删除用户 + 清理用户-角色关联（保留审计痕迹）
        LocalDateTime now = LocalDateTime.now();
        userMapper.logicDelete(id, operator, now);
        userRoleMapper.logicDeleteByUserId(id, operator);
        // 注销令牌 + 清理缓存（提交后执行，避免并发读回填旧快照）
        String deletedUserName = user.getUserName();
        afterCommitExecutor.run(() -> {
            jwtTokenService.revokeToken(deletedUserName);
            rbacCacheService.evictUser(id);
        });
        log.info("删除用户成功: id={}, userName={}, operator={}", id, user.getUserName(), operator);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchUpdateStatus(List<Long> ids, Integer status, String operator) {
        if (CollectionUtils.isEmpty(ids)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请选择至少一个用户");
        }
        if (status == null || (status != 0 && status != 1)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "状态只能是 0 或 1");
        }
        List<String> revokedUsers = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        for (Long id : ids) {
            SysAuthUser user = userMapper.selectById(id);
            if (user == null) {
                throw new BusinessException(ResultCode.DATA_NOT_EXIST, "用户不存在: " + id);
            }
            if (status == 1 && (Long.valueOf(1L).equals(id) || userHasAdminRole(id))) {
                throw new BusinessException(ResultCode.BUILT_IN_DATA, "超级管理员账号不可被禁用");
            }
            SysAuthUser patch = new SysAuthUser();
            patch.setId(id);
            patch.setStatus(status);
            patch.setUpdateBy(operator);
            patch.setUpdateTime(now);
            userMapper.updateSelective(patch);
            if (status == 1) {
                revokedUsers.add(user.getUserName());
            }
        }
        afterCommitExecutor.run(() -> {
            for (Long id : ids) {
                rbacCacheService.evictUser(id);
            }
            for (String userName : revokedUsers) {
                jwtTokenService.revokeToken(userName);
            }
        });
        log.info("批量更新用户状态完成: count={}, status={}, operator={}", ids.size(), status, operator);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void batchDelete(List<Long> ids, String operator) {
        if (CollectionUtils.isEmpty(ids)) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "请选择至少一个用户");
        }
        for (Long id : ids) {
            deleteUser(id, operator);
        }
        log.info("批量删除用户完成: count={}, operator={}", ids.size(), operator);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetPassword(ResetPasswordRequest request, String operator) {
        SysAuthUser user = userMapper.selectById(request.getUserId());
        if (user == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "用户不存在");
        }
        // 管理员重置密码同样受密码策略约束（否则会留下绕过复杂度的后门）
        passwordPolicy.validateComplexity(request.getNewPassword());
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdateBy(operator);
        user.setUpdateTime(LocalDateTime.now());
        userMapper.updateSelective(user);
        // 强制被重置用户重新登录（提交后执行：避免密码尚未落库就先踢人）
        Long resetUserId = user.getId();
        String resetUserName = user.getUserName();
        afterCommitExecutor.run(() -> {
            jwtTokenService.revokeToken(resetUserName);
            rbacCacheService.evictUser(resetUserId);
        });
        log.info("重置用户密码成功: id={}, operator={}", user.getId(), operator);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public UserRoleVO getUserRoles(Long userId) {
        if (userMapper.selectById(userId) == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "用户不存在");
        }
        UserRoleVO vo = new UserRoleVO();
        vo.setUserId(userId);
        vo.setRoleIds(userRoleMapper.selectRoleIdsByUserId(userId));
        return vo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void assignRoles(AssignRoleRequest request, String operator) {
        Long userId = request.getUserId();
        SysAuthUser user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.DATA_NOT_EXIST, "用户不存在");
        }
        List<Long> roleIds = request.getRoleIds() == null
                ? Collections.emptyList()
                : request.getRoleIds().stream().distinct().collect(Collectors.toList());
        // 角色存在性校验（结果复用给下面的超级管理员判定，避免重复查库）
        List<SysAuthRole> roles = CollectionUtils.isEmpty(roleIds)
                ? Collections.emptyList()
                : roleMapper.selectByIds(roleIds);
        if (roles.size() != roleIds.size()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "包含不存在的角色");
        }
        // 安全规则：内置管理员账号（id=1）必须始终保留超级管理员角色
        boolean targetIsAdmin = userHasAdminRole(userId);
        boolean newSetKeepsAdmin = roles.stream().anyMatch(r -> isAdminRoleKey(r.getRoleKey()));
        if (targetIsAdmin && !newSetKeepsAdmin) {
            if (Long.valueOf(1L).equals(userId)) {
                throw new BusinessException(ResultCode.BUILT_IN_DATA, "内置管理员账号必须保留超级管理员角色");
            }
            // 允许把其他用户移出管理员（真实系统中应保证至少还有一名管理员，此处记录日志以便审计）
            log.warn("用户 {} 被移出超级管理员角色: operator={}", userId, operator);
        }
        // 全量覆盖
        replaceUserRoles(userId, roleIds, operator);
        // 角色变更清理缓存（提交后执行，避免并发读回填旧角色集合）
        afterCommitExecutor.run(() -> rbacCacheService.evictUser(userId));
    }

    /* ------------------------------ 内部方法 ------------------------------ */

    /** 用户是否拥有超级管理员角色 */
    private boolean userHasAdminRole(Long userId) {
        return userRbacService.listRolesByUserId(userId).stream()
                .anyMatch(r -> isAdminRoleKey(r.getRoleKey()));
    }

    /** 是否为超级角色 key */
    private boolean isAdminRoleKey(String roleKey) {
        return properties.getSecurity().getAdminRoleKey().equals(roleKey);
    }

    /**
     * 校验角色 ID 全部存在（逻辑删除的角色视为不存在）。
     *
     * <p>新增/编辑用户时若不校验，可写入指向不存在角色的 user_role 关联；
     * 该脏数据会让用户列表装载角色时出现 null 元素并在组装 VO 时抛 NPE。</p>
     *
     * @param roleIds 角色 ID 列表（可含 null/重复）
     */
    private void checkRoleIdsExist(List<Long> roleIds) {
        List<Long> distinctIds = roleIds.stream()
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (distinctIds.isEmpty()) {
            return;
        }
        if (roleMapper.selectByIds(distinctIds).size() != distinctIds.size()) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "包含不存在的角色");
        }
    }

    /** 业务字段拷贝（账号、密码除外） */
    private void applyBaseFields(UserSaveRequest req, SysAuthUser user) {
        // 分配部门前先确认部门存在（未加外键：逻辑删除让外键语义变复杂，
        // 因此在应用层做存在性校验，避免把用户挂到不存在的部门上）
        if (req.getDeptId() != null && deptMapper.selectById(req.getDeptId()) == null) {
            throw new BusinessException(ResultCode.PARAM_ERROR, "所属部门不存在");
        }
        user.setNickName(req.getNickName());
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
        user.setSex(req.getSex());
        user.setAvatar(req.getAvatar());
        user.setStatus(req.getStatus());
        user.setDeptId(req.getDeptId());
        user.setIntroduce(req.getIntroduce());
    }

    /**
     * 全量覆盖用户角色：旧关联逻辑删除 → 插入新关联。
     *
     * <p>插入前过滤 null 并去重：重复的 roleId 会产生多条关联，
     * 使列表页角色列重复显示；null 则形成无意义脏关联。</p>
     */
    private void replaceUserRoles(Long userId, List<Long> roleIds, String operator) {
        // 旧关联逻辑删除
        userRoleMapper.logicDeleteByUserId(userId, operator);
        // 过滤 null + 去重后再插入
        List<Long> validRoleIds = roleIds == null ? Collections.emptyList()
                : roleIds.stream().filter(Objects::nonNull).distinct().collect(Collectors.toList());
        // 插入新关联
        LocalDateTime now = LocalDateTime.now();
        for (Long roleId : validRoleIds) {
            SysAuthUserRole link = new SysAuthUserRole();
            link.setUserId(userId);
            link.setRoleId(roleId);
            link.setCreatedBy(operator);
            link.setCreatedTime(now);
            userRoleMapper.insert(link);
        }
        log.info("覆盖分配用户角色: userId={}, roleIds={}, operator={}", userId, validRoleIds, operator);
    }

    /** 批量加载用户-角色映射（供列表页使用，减少查询次数） */
    /**
     * 批量查部门名（用户列表展示"部门"列）。
     *
     * <p>与角色一样走"批量补"而不是 JOIN：分页结果最多 500 行，
     * 一次 IN 查询比给主查询加 JOIN 更简单，也不影响分页 count 的写法。</p>
     *
     * @param users 当前页用户
     * @return deptId → deptName
     */
    private Map<Long, String> loadDeptNames(List<SysAuthUser> users) {
        if (CollectionUtils.isEmpty(users)) {
            return Collections.emptyMap();
        }
        List<Long> deptIds = users.stream()
                .map(SysAuthUser::getDeptId)
                .filter(Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());
        if (deptIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<SysDept> depts = deptMapper.selectByIds(deptIds);
        if (CollectionUtils.isEmpty(depts)) {
            return Collections.emptyMap();
        }
        return depts.stream().collect(Collectors.toMap(SysDept::getId, SysDept::getDeptName, (a, b) -> a));
    }
    private Map<Long, List<SysAuthRole>> loadRolesMap(List<Long> userIds) {
        Map<Long, List<SysAuthRole>> result = new HashMap<>();
        if (CollectionUtils.isEmpty(userIds)) {
            return result;
        }
        List<SysAuthUserRole> links = userRoleMapper.selectByUserIds(userIds);
        if (CollectionUtils.isEmpty(links)) {
            return result;
        }
        List<Long> roleIds = links.stream().map(SysAuthUserRole::getRoleId)
                .filter(Objects::nonNull)
                .distinct().collect(Collectors.toList());
        if (roleIds.isEmpty()) {
            return result;
        }
        Map<Long, SysAuthRole> roleMap = roleMapper.selectByIds(roleIds).stream()
                .collect(Collectors.toMap(SysAuthRole::getId, r -> r));
        links.forEach(l -> {
            // 角色查不到（如角色已逻辑删除但关联残留）时跳过，避免向列表塞入 null 导致组装 VO 时 NPE
            SysAuthRole role = roleMap.get(l.getRoleId());
            if (role != null) {
                result.computeIfAbsent(l.getUserId(), k -> new ArrayList<>()).add(role);
            }
        });
        return result;
    }

    /** 用户实体 → VO */
    private UserVO toVO(SysAuthUser user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUserName(user.getUserName());
        vo.setNickName(user.getNickName());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setSex(user.getSex());
        vo.setAvatar(user.getAvatar());
        vo.setStatus(user.getStatus());
        vo.setIntroduce(user.getIntroduce());
        vo.setCreatedBy(user.getCreatedBy());
        vo.setCreatedTime(user.getCreatedTime());
        vo.setUpdateTime(user.getUpdateTime());
        return vo;
    }
}
