package com.admintem.web.service.impl;

import com.admintem.web.common.ResultCode;
import com.admintem.web.entity.SysAuthPermission;
import com.admintem.web.entity.SysAuthRolePermission;
import com.admintem.web.entity.dto.PermissionSaveRequest;
import com.admintem.web.exception.BusinessException;
import com.admintem.web.mapper.SysAuthPermissionMapper;
import com.admintem.web.mapper.SysAuthRolePermissionMapper;
import com.admintem.web.security.RbacCacheService;
import com.admintem.web.support.AfterCommitExecutor;
import com.admintem.web.service.UserRbacService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Spy;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link SysAuthPermissionServiceImpl} 分支逻辑单元测试（全 Mock，不连数据库）。
 *
 * <p>重点覆盖两处曾经修复过的严重缺陷：</p>
 * <ol>
 *   <li><b>父子环</b>：编辑权限时把父级设为自己的子孙，会让环节点在树中静默消失，
 *       并使级联删除递归溢出；</li>
 *   <li><b>级联删除</b>：删除菜单时需连带子孙节点与其角色关联，并清理受影响用户的权限缓存。</li>
 * </ol>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("权限管理服务（分支逻辑）")
class SysAuthPermissionServiceImplTest {

    @Mock
    private SysAuthPermissionMapper permissionMapper;
    @Mock
    private SysAuthRolePermissionMapper rolePermissionMapper;
    @Mock
    private UserRbacService userRbacService;
    @Mock
    private RbacCacheService rbacCacheService;

    /** 真实实例（非 mock）：无事务时立即执行，保持既有断言语义 */
    @Spy
    private AfterCommitExecutor afterCommitExecutor = new AfterCommitExecutor();

    @InjectMocks
    private SysAuthPermissionServiceImpl service;

    private static final String OPERATOR = "admin";

    private SysAuthPermission permission(Long id, Long parentId, String key) {
        SysAuthPermission p = new SysAuthPermission();
        p.setId(id);
        p.setParentId(parentId);
        p.setName("节点" + id);
        p.setType(0);
        p.setStatus(0);
        p.setShow(0);
        p.setPermissionKey(key);
        return p;
    }

    private PermissionSaveRequest saveRequest(Long id, Long parentId, String key) {
        PermissionSaveRequest req = new PermissionSaveRequest();
        req.setId(id);
        req.setParentId(parentId);
        req.setName("节点");
        req.setType(1);
        req.setPermissionKey(key);
        req.setStatus(0);
        return req;
    }

    /* --------------------------- 父子环防护 --------------------------- */

    @Test
    @DisplayName("编辑：父级不能是自己")
    void updateRejectsSelfAsParent() {
        when(permissionMapper.selectById(5L)).thenReturn(permission(5L, 2L, "userInfo:permissionInfo"));

        assertThatThrownBy(() -> service.updatePermission(saveRequest(5L, 5L, "userInfo:permissionInfo"), OPERATOR))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("父级不能是自己");

        verify(permissionMapper, never()).updateSelective(any());
    }

    @Test
    @DisplayName("编辑：父级不能是自己的子孙（防止父子成环）")
    void updateRejectsDescendantAsParent() {
        // 节点 2（系统管理）的子孙是 3（用户管理）
        when(permissionMapper.selectById(2L)).thenReturn(permission(2L, 0L, "system:management"));
        when(permissionMapper.selectById(3L)).thenReturn(permission(3L, 2L, "userInfo:userInfo"));

        // 把 2 的父级改成 3（即自己的子级）→ 应被拒绝
        assertThatThrownBy(() -> service.updatePermission(saveRequest(2L, 3L, "system:management"), OPERATOR))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("父级不能是自己的子级");

        verify(permissionMapper, never()).updateSelective(any());
    }

    @Test
    @DisplayName("编辑：父级不存在时拒绝")
    void updateRejectsMissingParent() {
        when(permissionMapper.selectById(9L)).thenReturn(permission(9L, 0L, "t:demo"));
        when(permissionMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> service.updatePermission(saveRequest(9L, 999L, "t:demo"), OPERATOR))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("父级权限不存在");
    }

    @Test
    @DisplayName("新增：父级不存在时拒绝")
    void createRejectsMissingParent() {
        when(permissionMapper.countByKey("t:new", null)).thenReturn(0L);
        when(permissionMapper.selectById(999L)).thenReturn(null);

        assertThatThrownBy(() -> service.createPermission(saveRequest(null, 999L, "t:new"), OPERATOR))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("父级权限不存在");

        verify(permissionMapper, never()).insert(any());
    }

    @Test
    @DisplayName("新增：权限标识重复时拒绝")
    void createRejectsDuplicateKey() {
        when(permissionMapper.countByKey("t:dup", null)).thenReturn(1L);

        assertThatThrownBy(() -> service.createPermission(saveRequest(null, 0L, "t:dup"), OPERATOR))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("权限标识已存在");
    }

    /* --------------------------- 级联删除 --------------------------- */

    @Test
    @DisplayName("删除：权限不存在时拒绝")
    void deleteRejectsMissingPermission() {
        when(permissionMapper.selectById(9L)).thenReturn(null);

        assertThatThrownBy(() -> service.deletePermission(9L, OPERATOR))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.DATA_NOT_EXIST.getCode());
    }

    @Test
    @DisplayName("删除：连带子孙节点一起逻辑删除，并清理角色关联与用户缓存")
    void deleteCascadesToDescendants() {
        // 结构：2(系统管理) → 3(用户管理) → 8(新增用户)
        List<SysAuthPermission> all = List.of(
                permission(2L, 0L, "system:management"),
                permission(3L, 2L, "userInfo:userInfo"),
                permission(8L, 3L, "sysAuthUser:addSysAuthUser"));
        when(permissionMapper.selectById(2L)).thenReturn(all.get(0));
        when(permissionMapper.selectAllOrdered()).thenReturn(all);

        // 角色关联：权限 2/3/8 关联到角色 1
        List<SysAuthRolePermission> links = new ArrayList<>();
        for (Long pid : List.of(2L, 3L, 8L)) {
            SysAuthRolePermission link = new SysAuthRolePermission();
            link.setRoleId(1L);
            link.setPermissionId(pid);
            links.add(link);
        }
        when(rolePermissionMapper.selectByPermissionIds(anyCollection())).thenReturn(links);
        // 受影响角色的用户
        when(userRbacService.listUserIdsByRoleIds(anyCollection())).thenReturn(List.of(1L));

        service.deletePermission(2L, OPERATOR);

        // 1) 级联集合应包含自身与全部子孙
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Long>> idsCaptor = ArgumentCaptor.forClass(List.class);
        verify(permissionMapper).logicDeleteByIds(idsCaptor.capture(), eq(OPERATOR), any());
        assertThat(idsCaptor.getValue()).containsExactlyInAnyOrder(2L, 3L, 8L);

        // 2) 角色关联被清理
        verify(rolePermissionMapper).logicDeleteByPermissionIds(anyCollection(), eq(OPERATOR));
        // 3) 受影响用户的权限快照被清理（按用户 ID 批量失效）
        verify(rbacCacheService).evictUsers(List.of(1L));
    }

    @Test
    @DisplayName("删除：无子孙时只删除自身")
    void deleteLeafOnlyRemovesItself() {
        when(permissionMapper.selectById(8L)).thenReturn(permission(8L, 3L, "sysAuthUser:addSysAuthUser"));
        when(permissionMapper.selectAllOrdered()).thenReturn(List.of(
                permission(8L, 3L, "sysAuthUser:addSysAuthUser")));
        when(rolePermissionMapper.selectByPermissionIds(anyCollection())).thenReturn(List.of());

        service.deletePermission(8L, OPERATOR);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Long>> idsCaptor = ArgumentCaptor.forClass(List.class);
        verify(permissionMapper).logicDeleteByIds(idsCaptor.capture(), eq(OPERATOR), any());
        assertThat(idsCaptor.getValue()).containsExactly(8L);
        verify(rbacCacheService, never()).evictUsers(anyCollection());
    }

    @Test
    @DisplayName("树查询：默认过滤禁用项，includeDisabled=true 时全部返回")
    void treeAllFiltersDisabledByDefault() {
        SysAuthPermission enabled = permission(1L, 0L, "home:homeview");
        SysAuthPermission disabled = permission(2L, 0L, "system:management");
        disabled.setStatus(1);
        when(permissionMapper.selectAllOrdered()).thenReturn(List.of(enabled, disabled));

        assertThat(service.treeAll(false)).extracting(vo -> vo.getId()).containsExactly(1L);
        assertThat(service.treeAll(true)).extracting(vo -> vo.getId()).containsExactly(1L, 2L);
    }
}
