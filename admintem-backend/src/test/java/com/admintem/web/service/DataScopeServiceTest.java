package com.admintem.web.service;

import com.admintem.web.entity.SysAuthRole;
import com.admintem.web.entity.SysAuthUser;
import com.admintem.web.entity.SysDept;
import com.admintem.web.entity.dto.UserPageQuery;
import com.admintem.web.mapper.SysAuthDeptMapper;
import com.admintem.web.mapper.SysAuthRoleMapper;
import com.admintem.web.mapper.SysAuthUserMapper;
import com.admintem.web.security.LoginUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DataScopeServiceTest {

    @Mock
    private SysAuthUserMapper userMapper;

    @Mock
    private SysAuthRoleMapper roleMapper;

    @Mock
    private SysAuthDeptMapper deptMapper;

    @InjectMocks
    private DataScopeService dataScopeService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void adminAlwaysGetsAllData() {
        loginAs(1L, Set.of("admin"));
        UserPageQuery query = new UserPageQuery();

        dataScopeService.apply(query);

        assertThat(query.getDataScope()).isEqualTo("ALL");
    }

    @Test
    void deptAndChildScopeContainsCurrentDeptAndDescendants() {
        loginAs(1L, Set.of("manager"));
        SysAuthRole role = new SysAuthRole();
        role.setDataScope("DEPT_AND_CHILD");
        when(roleMapper.selectByRoleKeys(Set.of("manager"))).thenReturn(List.of(role));

        SysAuthUser user = new SysAuthUser();
        user.setId(1L);
        user.setDeptId(10L);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(deptMapper.selectAllOrdered()).thenReturn(List.of(
                dept(10L, 0L), dept(11L, 10L), dept(12L, 11L), dept(20L, 0L)));

        UserPageQuery query = new UserPageQuery();
        dataScopeService.apply(query);

        assertThat(query.getDataScope()).isEqualTo("DEPT_AND_CHILD");
        assertThat(query.getDataScopeDeptIds()).containsExactly(10L, 11L, 12L);
    }

    @Test
    void noDeptFallsBackToSelf() {
        loginAs(1L, Set.of("common"));
        SysAuthRole role = new SysAuthRole();
        role.setDataScope("DEPT");
        when(roleMapper.selectByRoleKeys(Set.of("common"))).thenReturn(List.of(role));
        when(userMapper.selectById(1L)).thenReturn(new SysAuthUser());

        UserPageQuery query = new UserPageQuery();
        dataScopeService.apply(query);

        assertThat(query.getDataScope()).isEqualTo("SELF");
        assertThat(query.getDataScopeUserId()).isEqualTo(1L);
    }

    private void loginAs(Long userId, Set<String> roleKeys) {
        LoginUser user = new LoginUser(userId, "u" + userId, "用户", null, 0);
        user.setRoleKeys(roleKeys);
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
    }

    private SysDept dept(Long id, Long parentId) {
        SysDept dept = new SysDept();
        dept.setId(id);
        dept.setParentId(parentId);
        return dept;
    }
}
