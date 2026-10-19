package com.admintem.web.entity.dto.validation;

import jakarta.validation.groups.Default;

/**
 * 校验分组定义。
 *
 * <p>同一个保存请求体（SaveRequest）同时服务于"新增"与"编辑"两个接口，
 * 但两者的必填字段并不相同：{@code id} 只在编辑时必填。
 * 直接用 {@code @NotNull} 会导致新增接口被误拦截，不用则编辑接口缺少校验。</p>
 *
 * <p>因此引入分组：</p>
 * <ul>
 *   <li>{@link Create}：新增场景；</li>
 *   <li>{@link Update}：编辑场景（额外要求 id 非空）。</li>
 * </ul>
 *
 * <p>两个分组都继承 {@link Default}，因此 {@code @Validated(Update.class)} 会同时校验
 * 默认组上的 {@code @NotBlank/@Size/@Pattern} 等约束，不会"只校验 id 而放过其它字段"。
 * 这一点由回归测试的第 16 组断言把关（编辑缺 id / 编辑字段为空 / 新增字段为空 三种情形）。</p>
 */
public final class ValidationGroups {

    private ValidationGroups() {
    }

    /** 新增场景（继承默认组，校验所有默认约束） */
    public interface Create extends Default {
    }

    /** 编辑场景（继承默认组，额外要求 id 非空） */
    public interface Update extends Default {
    }
}
