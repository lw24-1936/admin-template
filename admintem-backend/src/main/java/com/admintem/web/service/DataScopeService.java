package com.admintem.web.service;

import com.admintem.web.entity.SysAuthRole;
import com.admintem.web.entity.SysAuthUser;
import com.admintem.web.entity.SysDept;
import com.admintem.web.entity.dto.UserPageQuery;
import com.admintem.web.mapper.SysAuthDeptMapper;
import com.admintem.web.mapper.SysAuthRoleMapper;
import com.admintem.web.mapper.SysAuthUserMapper;
import com.admintem.web.security.LoginUser;
import com.admintem.web.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 行级数据权限解析器（当前用于用户列表；后续业务查询复用同一 Scope 值）。
 *
 * <p>数据范围来自角色 {@code sys_auth_role.data_scope}，多角色取最宽范围：</p>
 * <pre>
 * ALL > DEPT_AND_CHILD > DEPT > SELF
 * </pre>
 *
 * <p>安全默认：没有角色、没有部门或范围为空时按 SELF 处理，避免误放开全量数据。</p>
 */
@Service
@RequiredArgsConstructor
public class DataScopeService {

    private static final String ALL = "ALL";
    private static final String DEPT = "DEPT";
    private static final String DEPT_AND_CHILD = "DEPT_AND_CHILD";
    private static final String SELF = "SELF";

    private final SysAuthUserMapper userMapper;
    private final SysAuthRoleMapper roleMapper;
    private final SysAuthDeptMapper deptMapper;

    /**
     * 将当前登录用户的数据范围写入用户分页查询条件。
     *
     * @param query 用户分页查询
     */
    public void apply(UserPageQuery query) {
        LoginUser current = SecurityUtils.currentUser();
        if (current.getRoleKeys() != null && current.getRoleKeys().contains("admin")) {
            query.setDataScope(ALL);
            return;
        }

        List<SysAuthRole> roles = current.getRoleKeys() == null || current.getRoleKeys().isEmpty()
                ? List.of()
                : roleMapper.selectByRoleKeys(current.getRoleKeys());
        String scope = broadestScope(roles);
        query.setDataScope(scope);
        query.setDataScopeUserId(current.getUserId());

        if (ALL.equals(scope)) {
            return;
        }
        if (SELF.equals(scope)) {
            return;
        }

        SysAuthUser currentUser = userMapper.selectById(current.getUserId());
        if (currentUser == null || currentUser.getDeptId() == null) {
            query.setDataScope(SELF);
            return;
        }
        if (DEPT.equals(scope)) {
            query.setDataScopeDeptIds(List.of(currentUser.getDeptId()));
        } else {
            query.setDataScopeDeptIds(collectDeptAndChildren(currentUser.getDeptId()));
        }
    }

    /**
     * 取多个角色中最宽的数据范围；空值按 ALL（兼容存量数据）处理。
     */
    private String broadestScope(List<SysAuthRole> roles) {
        int best = -1;
        String result = ALL;
        for (SysAuthRole role : roles) {
            String scope = role.getDataScope();
            if (scope == null || scope.isBlank()) {
                scope = ALL;
            }
            int weight = switch (scope) {
                case ALL -> 4;
                case DEPT_AND_CHILD -> 3;
                case DEPT -> 2;
                case SELF -> 1;
                default -> 0;
            };
            if (weight > best) {
                best = weight;
                result = scope;
            }
        }
        return result;
    }

    /**
     * 收集部门本身及全部后代部门 ID（部门树规模很小，内存遍历足够）。
     */
    private List<Long> collectDeptAndChildren(Long rootId) {
        List<SysDept> all = deptMapper.selectAllOrdered();
        Map<Long, List<Long>> children = new HashMap<>();
        for (SysDept dept : all) {
            children.computeIfAbsent(dept.getParentId(), key -> new ArrayList<>()).add(dept.getId());
        }
        List<Long> result = new ArrayList<>();
        collect(rootId, children, result);
        return result;
    }

    private void collect(Long id, Map<Long, List<Long>> children, List<Long> result) {
        result.add(id);
        for (Long child : children.getOrDefault(id, List.of())) {
            collect(child, children, result);
        }
    }
}
