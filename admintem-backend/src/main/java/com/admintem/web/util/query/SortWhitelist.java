package com.admintem.web.util.query;

import java.util.Locale;
import java.util.Map;

/**
 * 列表排序字段白名单工具。
 *
 * <p>列表接口允许前端传"逻辑字段名 + 方向"（如 {@code userName + asc}），
 * 但**绝不能**把用户输入直接拼进 SQL。本工具的做法是：</p>
 * <ol>
 *   <li>每个资源维护一份 {@code 逻辑字段名 -> 数据库列名} 的白名单（见各 Service 常量）；</li>
 *   <li>请求字段未登记时返回 {@code null}，由调用方回落到默认排序（不报错，避免把内部列名暴露给前端）；</li>
 *   <li>方向只接受 {@code asc/desc}，其余一律按 {@code ASC} 处理；</li>
 *   <li>返回值仅由白名单中的列名与固定方向拼成，因此可以安全地以 {@code ${orderBy}} 注入 XML。</li>
 * </ol>
 *
 * <p>示例：{@code SortWhitelist.resolve("userName", "desc", USER_SORT_WHITELIST)} → {@code "user_name DESC"}</p>
 */
public final class SortWhitelist {

    private SortWhitelist() {
    }

    /**
     * 解析排序子句。
     *
     * @param sortField 前端传入的逻辑字段名（可为空）
     * @param sortOrder 前端传入的方向（asc/desc，可为空，默认 asc）
     * @param whitelist 白名单：逻辑字段名 → 数据库列名
     * @return 形如 {@code "user_name DESC"} 的排序子句；字段非法或无白名单时返回 null（调用方回落默认排序）
     */
    public static String resolve(String sortField, String sortOrder, Map<String, String> whitelist) {
        if (sortField == null || sortField.isBlank() || whitelist == null || whitelist.isEmpty()) {
            return null;
        }
        String column = whitelist.get(sortField.trim());
        if (column == null) {
            return null;
        }
        String direction = "desc".equalsIgnoreCase(sortOrder == null ? "" : sortOrder.trim())
                ? "DESC" : "ASC";
        return column + " " + direction;
    }

    /**
     * 请求的排序字段是否被白名单接受（用于日志区分"未传"与"传了非法值"）。
     *
     * @param sortField 前端传入的逻辑字段名
     * @param whitelist 白名单
     * @return true 表示字段在白名单内
     */
    public static boolean accepted(String sortField, Map<String, String> whitelist) {
        if (sortField == null || sortField.isBlank() || whitelist == null) {
            return false;
        }
        return whitelist.containsKey(sortField.trim().toLowerCase(Locale.ROOT))
                || whitelist.containsKey(sortField.trim());
    }
}
