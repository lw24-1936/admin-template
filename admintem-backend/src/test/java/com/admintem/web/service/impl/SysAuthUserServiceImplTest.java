package com.admintem.web.service.impl;

import com.admintem.web.common.ResultCode;
import com.admintem.web.config.AdmintemProperties;
import com.admintem.web.entity.SysAuthRole;
import com.admintem.web.entity.SysAuthUser;
import com.admintem.web.entity.dto.UserSaveRequest;
import com.admintem.web.exception.BusinessException;
import com.admintem.web.mapper.SysAuthRoleMapper;
import com.admintem.web.mapper.SysAuthUserMapper;
import com.admintem.web.mapper.SysAuthUserRoleMapper;
import com.admintem.web.security.JwtTokenService;
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
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * {@link SysAuthUserServiceImpl} 分支逻辑单元测试（全部使用 Mock，不连数据库）。
 *
 * <p>覆盖三类"越权/脏数据"防线：账号唯一性、内置管理员保护、角色 ID 存在性校验，
 * 以及成功路径的关键副作用（密码加密、角色关联先清后写、令牌吊销与缓存清理）。</p>
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("用户管理服务（分支逻辑）")
class SysAuthUserServiceImplTest {

    @Mock
    private SysAuthUserMapper userMapper;
    @Mock
    private SysAuthRoleMapper roleMapper;
    @Mock
    private SysAuthUserRoleMapper userRoleMapper;
    @Mock
    private UserRbacService userRbacService;
    @Mock
    private RbacCacheService rbacCacheService;
    @Mock
    private JwtTokenService jwtTokenService;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AdmintemProperties properties;

    /** 真实实例（非 mock）：无事务时立即执行，保持既有断言语义 */
    @Spy
    private AfterCommitExecutor afterCommitExecutor = new AfterCommitExecutor();

    /**
     * 真实密码策略实例（自带全新配置：复杂度默认关闭）。
     *
     * <p>这里不能用 {@code properties} mock：{@code PasswordPolicy} 会读
     * {@code getAuth().getPassword()}，mock 的嵌套对象为 null 会 NPE。
     * 用真实默认配置既能执行到"复杂度关闭即放行"的分支，也不影响既有断言。</p>
     */
    @Spy
    private com.admintem.web.security.PasswordPolicy passwordPolicy =
            new com.admintem.web.security.PasswordPolicy(new AdmintemProperties());

    @InjectMocks
    private SysAuthUserServiceImpl service;

    private static final String OPERATOR = "admin";

    @BeforeEach
    void setUp() {
        // 超级角色 key 取默认约定值
        AdmintemProperties.Security security = new AdmintemProperties.Security();
        security.setAdminRoleKey("admin");
        when(properties.getSecurity()).thenReturn(security);
        lenient().when(passwordEncoder.encode(anyString())).thenReturn("ENCODED");
    }

    /** 构造新增请求 */
    private UserSaveRequest newUserRequest() {
        UserSaveRequest req = new UserSaveRequest();
        req.setUserName("t_new");
        req.setNickName("新用户");
        req.setPassword("Passw0rd!");
        return req;
    }

    private SysAuthRole role(Long id, String roleKey) {
        SysAuthRole role = new SysAuthRole();
        role.setId(id);
        role.setRoleKey(roleKey);
        role.setRoleName(roleKey);
        return role;
    }

    /* ------------------------------ 新增 ------------------------------ */

    @Test
    @DisplayName("新增：账号已存在时拒绝（用户名重复）")
    void createUserRejectsDuplicateUserName() {
        when(userMapper.countByUserName("t_new", null)).thenReturn(1L);

        assertThatThrownBy(() -> service.createUser(newUserRequest(), OPERATOR))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.USERNAME_EXISTS.getCode());

        verify(userMapper, never()).insert(any());
    }

    @Test
    @DisplayName("新增：未填初始密码时拒绝（参数错误）")
    void createUserRejectsMissingPassword() {
        when(userMapper.countByUserName("t_new", null)).thenReturn(0L);
        UserSaveRequest req = newUserRequest();
        req.setPassword(null);

        assertThatThrownBy(() -> service.createUser(req, OPERATOR))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("必须填写初始密码");

        verify(userMapper, never()).insert(any());
    }

    @Test
    @DisplayName("新增成功：密码加密、状态默认启用、账号写入并返回主键")
    void createUserPersistsWithEncodedPassword() {
        when(userMapper.countByUserName("t_new", null)).thenReturn(0L);
        // 模拟 insert 回填主键
        when(userMapper.insert(any(SysAuthUser.class))).thenAnswer(inv -> {
            SysAuthUser u = inv.getArgument(0);
            u.setId(100L);
            return 1;
        });

        Long id = service.createUser(newUserRequest(), OPERATOR);

        assertThat(id).isEqualTo(100L);
        verify(passwordEncoder).encode("Passw0rd!");
        verify(userMapper).insert(any(SysAuthUser.class));
    }

    @Test
    @DisplayName("新增：角色 ID 含不存在项时拒绝（避免写入脏关联）")
    void createUserRejectsUnknownRoleIds() {
        when(userMapper.countByUserName("t_new", null)).thenReturn(0L);
        when(userMapper.insert(any(SysAuthUser.class))).thenAnswer(inv -> {
            ((SysAuthUser) inv.getArgument(0)).setId(100L);
            return 1;
        });
        // 只查回 1 个角色，但请求传了 2 个
        when(roleMapper.selectByIds(any())).thenReturn(List.of(role(1L, "common")));

        UserSaveRequest req = newUserRequest();
        req.setRoleIds(List.of(1L, 999L));

        assertThatThrownBy(() -> service.createUser(req, OPERATOR))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("包含不存在的角色");
    }

    @Test
    @DisplayName("新增：带合法角色时先逻辑删除旧关联再插入新关联")
    void createUserReplacesRoleLinks() {
        when(userMapper.countByUserName("t_new", null)).thenReturn(0L);
        when(userMapper.insert(any(SysAuthUser.class))).thenAnswer(inv -> {
            ((SysAuthUser) inv.getArgument(0)).setId(100L);
            return 1;
        });
        when(roleMapper.selectByIds(any())).thenReturn(List.of(role(1L, "common"), role(2L, "admin")));

        UserSaveRequest req = newUserRequest();
        req.setRoleIds(List.of(1L, 2L));

        service.createUser(req, OPERATOR);

        verify(userRoleMapper).logicDeleteByUserId(100L, OPERATOR);
        verify(userRoleMapper, times(2)).insert(any());
    }

    /* ------------------------------ 编辑 ------------------------------ */

    @Test
    @DisplayName("编辑：用户不存在时拒绝")
    void updateUserRejectsMissingUser() {
        when(userMapper.selectById(9L)).thenReturn(null);
        UserSaveRequest req = new UserSaveRequest();
        req.setId(9L);

        assertThatThrownBy(() -> service.updateUser(req, OPERATOR))
                .isInstanceOf(BusinessException.class)
                .extracting(e -> ((BusinessException) e).getCode())
                .isEqualTo(ResultCode.DATA_NOT_EXIST.getCode());
    }

    @Test
    @DisplayName("编辑：不允许修改登录账号")
    void updateUserRejectsUserNameChange() {
        SysAuthUser existing = new SysAuthUser();
        existing.setId(9L);
        existing.setUserName("t_old");
        when(userMapper.selectById(9L)).thenReturn(existing);

        UserSaveRequest req = new UserSaveRequest();
        req.setId(9L);
        req.setUserName("t_new_name");

        assertThatThrownBy(() -> service.updateUser(req, OPERATOR))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("登录账号不可修改");
    }

    @Test
    @DisplayName("编辑：内置管理员（id=1）不可被禁用")
    void updateUserRejectsDisablingBuiltInAdmin() {
        SysAuthUser admin = new SysAuthUser();
        admin.setId(1L);
        admin.setUserName("admin");
        when(userMapper.selectById(1L)).thenReturn(admin);

        UserSaveRequest req = new UserSaveRequest();
        req.setId(1L);
        req.setUserName("admin");
        req.setStatus(1);

        assertThatThrownBy(() -> service.updateUser(req, OPERATOR))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("超级管理员账号不可被禁用");
    }

    @Test
    @DisplayName("编辑：拥有 admin 角色的用户不可被禁用")
    void updateUserRejectsDisablingAdminRoleUser() {
        SysAuthUser user = new SysAuthUser();
        user.setId(9L);
        user.setUserName("t_admin");
        when(userMapper.selectById(9L)).thenReturn(user);
        when(userRbacService.listRolesByUserId(9L)).thenReturn(List.of(role(2L, "admin")));

        UserSaveRequest req = new UserSaveRequest();
        req.setId(9L);
        req.setUserName("t_admin");
        req.setStatus(1);

        assertThatThrownBy(() -> service.updateUser(req, OPERATOR))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("超级管理员账号不可被禁用");
    }

    @Test
    @DisplayName("编辑：禁用普通用户时吊销令牌并清理缓存")
    void updateUserDisablingRevokesToken() {
        SysAuthUser user = new SysAuthUser();
        user.setId(9L);
        user.setUserName("t_user");
        when(userMapper.selectById(9L)).thenReturn(user);
        when(userRbacService.listRolesByUserId(9L)).thenReturn(List.of(role(3L, "common")));

        UserSaveRequest req = new UserSaveRequest();
        req.setId(9L);
        req.setUserName("t_user");
        req.setStatus(1);

        service.updateUser(req, OPERATOR);

        verify(userMapper).updateSelective(any(SysAuthUser.class));
        verify(rbacCacheService).evictUser(9L);
        verify(jwtTokenService).revokeToken("t_user");
    }

    /* ------------------------------ 删除 ------------------------------ */

    @Test
    @DisplayName("删除：内置管理员账号不可删除")
    void deleteUserRejectsBuiltInAdmin() {
        SysAuthUser admin = new SysAuthUser();
        admin.setId(1L);
        admin.setUserName("admin");
        when(userMapper.selectById(1L)).thenReturn(admin);

        assertThatThrownBy(() -> service.deleteUser(1L, OPERATOR))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("内置管理员账号不可删除");

        verify(userMapper, never()).logicDelete(any(), anyString(), any());
    }

    @Test
    @DisplayName("删除：拥有 admin 角色的用户不可删除")
    void deleteUserRejectsAdminRoleUser() {
        SysAuthUser user = new SysAuthUser();
        user.setId(9L);
        user.setUserName("t_admin");
        when(userMapper.selectById(9L)).thenReturn(user);
        when(userRbacService.listRolesByUserId(9L)).thenReturn(List.of(role(2L, "admin")));

        assertThatThrownBy(() -> service.deleteUser(9L, OPERATOR))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("拥有超级管理员角色的用户不可删除");
    }

    @Test
    @DisplayName("删除成功：逻辑删除用户、清理关联、吊销令牌并清缓存")
    void deleteUserCleansUpRelatedState() {
        SysAuthUser user = new SysAuthUser();
        user.setId(9L);
        user.setUserName("t_user");
        when(userMapper.selectById(9L)).thenReturn(user);
        when(userRbacService.listRolesByUserId(9L)).thenReturn(List.of(role(3L, "common")));

        service.deleteUser(9L, OPERATOR);

        verify(userMapper).logicDelete(eq(9L), eq(OPERATOR), any());
        verify(userRoleMapper).logicDeleteByUserId(9L, OPERATOR);
        verify(jwtTokenService).revokeToken("t_user");
        verify(rbacCacheService).evictUser(9L);
    }
}
