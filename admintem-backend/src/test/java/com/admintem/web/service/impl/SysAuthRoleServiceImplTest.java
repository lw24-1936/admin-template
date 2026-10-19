package com.admintem.web.service.impl;

import com.admintem.web.common.ResultCode;
import com.admintem.web.config.AdmintemProperties;
import com.admintem.web.entity.SysAuthRole;
import com.admintem.web.entity.dto.RoleSaveRequest;
import com.admintem.web.exception.BusinessException;
import com.admintem.web.mapper.SysAuthPermissionMapper;
import com.admintem.web.mapper.SysAuthRoleMapper;
import com.admintem.web.mapper.SysAuthRolePermissionMapper;
import com.admintem.web.mapper.SysAuthUserRoleMapper;
import com.admintem.web.security.RbacCacheService;
import com.admintem.web.support.AfterCommitExecutor;
import com.admintem.web.service.UserRbacService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link SysAuthRoleServiceImpl} 分支逻辑单元测试（全 Mock，不连数据库）。
 *
 * <p>角色是权限体系的枢纽：内置超级角色不可删改标识、仍绑定用户的角色不可删，
 * 这些守卫一旦失效会导致"管理员把自己锁在门外"，因此单独用单测锁定。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("角色管理服务（分支逻辑）")
class SysAuthRoleServiceImplTest {

    @Mock
    private SysAuthRoleMapper roleMapper;
    @Mock
    private SysAuthUserRoleMapper userRoleMapper;
    @Mock
    private SysAuthRolePermissionMapper rolePermissionMapper;
    @Mock
    private SysAuthPermissionMapper permissionMapper;
    @Mock
    private UserRbacService userRbacService;
    @Mock
    private RbacCacheService rbacCacheService;
    @Mock
    private AdmintemProperties properties;

    /** 真实实例（非 mock）：无事务时立即执行，保持既有断言语义 */
    @Spy
    private AfterCommitExecutor afterCommitExecutor = new AfterCommitExecutor();

    @InjectMocks
    private SysAuthRoleServiceImpl service;

    private static final String OPERATOR = "admin";

    @BeforeEach
    void setUp() {
        AdmintemProperties.Security security = new AdmintemProperties.Security();
        security.setAdminRoleKey("admin");
        when(properties.getSecurity()).thenReturn(security);
    }

    private SysAuthRole role(Long id, String roleKey) {
        SysAuthRole role = new SysAuthRole();
        role.setId(id);
        role.setRoleKey(roleKey);
        role.setRoleName(roleKey);
        return role;
    }

    private RoleSaveRequest saveRequest(Long id, String roleName, String roleKey) {
        RoleSaveRequest req = new RoleSaveRequest();
        req.setId(id);
        req.setRoleName(roleName);
        req.setRoleKey(roleKey);
        return req;
    }

    /* ------------------------------ 新增 ------------------------------ */

    @Test
    @DisplayName("新增：角色标识重复时拒绝")
    void createRoleRejectsDuplicateRoleKey() {
        when(roleMapper.countByRoleKey("t_demo", null)).thenReturn(1L);

        assertThatThrownBy(() -> service.createRole(saveRequest(null, "演示角色", "t_demo"), OPERATOR))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("角色标识已存在");

        verify(roleMapper, never()).insert(any());
    }

    @Test
    @DisplayName("新增成功：写入角色并返回主键")
    void createRolePersistsAndReturnsId() {
        when(roleMapper.countByRoleKey("t_demo", null)).thenReturn(0L);
        when(roleMapper.insert(any(SysAuthRole.class))).thenAnswer(inv -> {
            ((SysAuthRole) inv.getArgument(0)).setId(66L);
            return 1;
        });

        Long id = service.createRole(saveRequest(null, "演示角色", "t_demo"), OPERATOR);

        assertThat(id).isEqualTo(66L);
        verify(roleMapper).insert(any(SysAuthRole.class));
    }

    /* ------------------------------ 编辑 ------------------------------ */

    @Test
    @DisplayName("编辑：角色不存在时拒绝")
    void updateRoleRejectsMissingRole() {
        when(roleMapper.selectById(9L)).thenReturn(null);

        assertThatThrownBy(() -> service.updateRole(saveRequest(9L, "x", "t_x"), OPERATOR))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("角色不存在");
    }

    @Test
    @DisplayName("编辑：内置超级角色的角色标识不可修改")
    void updateRoleRejectsChangingAdminRoleKey() {
        when(roleMapper.selectById(1L)).thenReturn(role(1L, "admin"));

        assertThatThrownBy(() -> service.updateRole(saveRequest(1L, "管理员", "superAdmin"), OPERATOR))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("内置超级角色标识不可修改");

        verify(roleMapper, never()).updateSelective(any());
    }

    /* ------------------------------ 删除 ------------------------------ */

    @Test
    @DisplayName("删除：内置超级角色不可删除")
    void deleteRoleRejectsBuiltInAdminRole() {
        when(roleMapper.selectById(1L)).thenReturn(role(1L, "admin"));

        assertThatThrownBy(() -> service.deleteRole(1L, OPERATOR))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("内置超级角色不可删除");

        verify(roleMapper, never()).logicDelete(any(), anyString(), any());
    }

    @Test
    @DisplayName("删除：角色仍绑定用户时拒绝（返回“仍被引用”业务码）")
    void deleteRoleRejectsWhenBoundToUsers() {
        when(roleMapper.selectById(2L)).thenReturn(role(2L, "cehsi1"));
        when(userRoleMapper.countByRoleId(2L)).thenReturn(3L);

        assertThatThrownBy(() -> service.deleteRole(2L, OPERATOR))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.DATA_IN_USE.getCode());

        verify(roleMapper, never()).logicDelete(any(), anyString(), any());
        verify(rolePermissionMapper, never()).logicDeleteByRoleId(any(), anyString());
    }

    @Test
    @DisplayName("删除成功：先清理权限关联再逻辑删除角色")
    void deleteRoleCleansRolePermissionLinks() {
        when(roleMapper.selectById(2L)).thenReturn(role(2L, "cehsi1"));
        when(userRoleMapper.countByRoleId(2L)).thenReturn(0L);

        service.deleteRole(2L, OPERATOR);

        verify(rolePermissionMapper).logicDeleteByRoleId(2L, OPERATOR);
        verify(roleMapper).logicDelete(eq(2L), eq(OPERATOR), any());
    }
}
