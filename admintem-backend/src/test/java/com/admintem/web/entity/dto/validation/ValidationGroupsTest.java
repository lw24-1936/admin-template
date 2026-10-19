package com.admintem.web.entity.dto.validation;

import com.admintem.web.entity.dto.PermissionSaveRequest;
import com.admintem.web.entity.dto.RoleSaveRequest;
import com.admintem.web.entity.dto.UserSaveRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 校验分组单元测试（新增/编辑必填字段差异）。
 *
 * <p>关键约定：{@link ValidationGroups.Create} 与 {@link ValidationGroups.Update}
 * 都继承 {@code Default}，因此用分组校验时**默认约束不能被跳过**——
 * 这一点如果被破坏，编辑接口会出现"只校验 id、放过空名称"的漏洞。
 * 这里直接用 Bean Validation API 断开 Spring 上下文验证该约定。</p>
 */
@DisplayName("新增/编辑校验分组")
class ValidationGroupsTest {

    private static ValidatorFactory factory;
    private static Validator validator;

    @BeforeAll
    static void setUp() {
        factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @AfterAll
    static void tearDown() {
        factory.close();
    }

    /** 统计指定分组下的违反项 */
    private Set<ConstraintViolation<Object>> validate(Object target, Class<?>... groups) {
        return validator.validate(target, groups);
    }

    @Test
    @DisplayName("角色新增：不要求 id，但要求 roleName/roleKey")
    void roleCreateGroupDoesNotRequireId() {
        RoleSaveRequest req = new RoleSaveRequest();
        req.setRoleName("测试角色");
        req.setRoleKey("t_demo");

        assertThat(validate(req, ValidationGroups.Create.class)).isEmpty();
    }

    @Test
    @DisplayName("角色编辑：缺少 id 时报“ID不能为空”")
    void roleUpdateGroupRequiresId() {
        RoleSaveRequest req = new RoleSaveRequest();
        req.setRoleName("测试角色");
        req.setRoleKey("t_demo");

        Set<ConstraintViolation<Object>> violations = validate(req, ValidationGroups.Update.class);
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).isEqualTo("ID不能为空");
    }

    @Test
    @DisplayName("分组继承 Default：编辑分组下默认约束（名称/标识必填）依然生效")
    void updateGroupStillValidatesDefaultConstraints() {
        RoleSaveRequest req = new RoleSaveRequest();
        req.setId(1L);
        req.setRoleName("");
        req.setRoleKey("");

        Set<ConstraintViolation<Object>> violations = validate(req, ValidationGroups.Update.class);
        assertThat(violations).extracting(v -> v.getPropertyPath().toString())
                .contains("roleName", "roleKey");
    }

    @Test
    @DisplayName("分组继承 Default：新增分组同样保留默认约束")
    void createGroupStillValidatesDefaultConstraints() {
        RoleSaveRequest req = new RoleSaveRequest();
        req.setRoleName("");

        Set<ConstraintViolation<Object>> violations = validate(req, ValidationGroups.Create.class);
        assertThat(violations).extracting(v -> v.getPropertyPath().toString())
                .contains("roleName", "roleKey");
    }

    @Test
    @DisplayName("用户编辑：id 必填；邮箱格式等默认约束一并生效")
    void userUpdateGroupRequiresIdAndValidatesEmail() {
        UserSaveRequest noId = new UserSaveRequest();
        noId.setUserName("t_demo");
        assertThat(validate(noId, ValidationGroups.Update.class))
                .extracting(v -> v.getMessage())
                .contains("ID不能为空");

        UserSaveRequest badEmail = new UserSaveRequest();
        badEmail.setId(1L);
        badEmail.setUserName("t_demo");
        badEmail.setEmail("not-an-email");
        assertThat(validate(badEmail, ValidationGroups.Update.class))
                .extracting(v -> v.getPropertyPath().toString())
                .contains("email");
    }

    @Test
    @DisplayName("权限编辑：id 必填")
    void permissionUpdateGroupRequiresId() {
        PermissionSaveRequest req = new PermissionSaveRequest();
        req.setName("测试权限");
        req.setType(1);

        assertThat(validate(req, ValidationGroups.Update.class))
                .extracting(v -> v.getMessage())
                .contains("ID不能为空");
    }

    @Test
    @DisplayName("不指定分组（默认组）时 id 不受约束——保证新增路径不受影响")
    void defaultGroupDoesNotRequireId() {
        RoleSaveRequest req = new RoleSaveRequest();
        req.setRoleName("测试角色");
        req.setRoleKey("t_demo");

        assertThat(validate(req)).isEmpty();
    }
}
