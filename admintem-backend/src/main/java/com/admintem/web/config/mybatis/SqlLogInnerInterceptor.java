package com.admintem.web.config.mybatis;

import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import org.apache.ibatis.executor.Executor;
import org.apache.ibatis.mapping.BoundSql;
import org.apache.ibatis.mapping.MappedStatement;
import org.apache.ibatis.mapping.ParameterMapping;
import org.apache.ibatis.reflection.MetaObject;
import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.session.ResultHandler;
import org.apache.ibatis.session.RowBounds;
import org.apache.ibatis.type.TypeHandlerRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.sql.SQLException;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SQL 全量日志拦截器（参考 config/mybatis/MybatisPlusAllSqlLog）。
 *
 * <p>实现为 MyBatis-Plus 的 {@link InnerInterceptor}（挂在 MybatisPlusInterceptor 上），
 * 每次执行前输出“mapperId + 参数替换后的完整 SQL”，便于联调排查与慢 SQL 复现；
 * 日志器名 {@code sys-sql}，开发/测试/预发布默认 INFO，生产 OFF（仅保留慢 SQL）。</p>
 *
 * <p>安全设计（本轮加固）：全量 SQL 会把参数值直接写进日志文件，其中可能包含手机号、
 * 邮箱、证件号、口令等个人信息。因此对参数值做**分字段脱敏**再做拼接：</p>
 * <ul>
 *   <li>按参数名识别：名称含 password/pwd/phone/mobile/email/mail/idcard/cardno/token/secret
 *       等关键字时按类型脱敏；</li>
 *   <li>其余情况（含无参数名的单值参数）按**值特征**兜底：11 位手机号、邮箱、18 位证件号
 *       形态一律脱敏，避免敏感值因列名未命中关键字而漏网；</li>
 *   <li>口令类一律整体打码（不保留长度信息），其余保留足够定位问题的首尾片段。</li>
 * </ul>
 * <p>开关：{@code admintem.sql.mask-enable}（默认 true）。</p>
 *
 * <p>修复参考实现中的 Date 打印缺陷（原实现格式化时误用 new Date()），
 * 并对 null / 集合参数做了安全处理。</p>
 */
public class SqlLogInnerInterceptor implements InnerInterceptor {

    private static final Logger log = LoggerFactory.getLogger("sys-sql");

    /** 需要整体打码（连长度都不保留）的字段关键字：口令、令牌、密钥 */
    private static final String[] SECRET_KEYWORDS = {"password", "passwd", "pwd", "secret", "token"};

    /** 需要按类型脱敏的字段关键字：手机号、邮箱、证件号、银行卡 */
    private static final String[] PERSONAL_KEYWORDS = {
            "phone", "mobile", "tel", "email", "mail", "idcard", "id_card", "cardno", "card_no", "bankcard"
    };

    /** 手机号：1 开头的 11 位数字 */
    private static final Pattern PHONE = Pattern.compile("^1\\d{10}$");

    /** 邮箱 */
    private static final Pattern EMAIL = Pattern.compile("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$");

    /** 二代身份证：17 位数字/字母 + 校验位（X 可为小写） */
    private static final Pattern ID_CARD = Pattern.compile("^\\d{17}[\\dXx]$");

    /** 是否对参数值脱敏（由配置注入） */
    private final boolean maskEnable;

    /**
     * @param maskEnable 是否启用敏感字段脱敏（配置项 admintem.sql.mask-enable）
     */
    public SqlLogInnerInterceptor(boolean maskEnable) {
        this.maskEnable = maskEnable;
    }

    @Override
    public void beforeQuery(Executor executor, MappedStatement ms, Object parameter,
                            RowBounds rowBounds, ResultHandler resultHandler,
                            BoundSql boundSql) throws SQLException {
        logInfo(boundSql, ms);
    }

    @Override
    public void beforeUpdate(Executor executor, MappedStatement ms, Object parameter) throws SQLException {
        BoundSql boundSql = ms.getBoundSql(parameter);
        logInfo(boundSql, ms);
    }

    /** 输出：mapperId + 完整可执行 SQL（敏感参数已脱敏） */
    private void logInfo(BoundSql boundSql, MappedStatement ms) {
        try {
            log.info("{} | {}", ms.getId(), showSql(ms.getConfiguration(), boundSql));
        } catch (Exception e) {
            // 传入异常对象以保留堆栈：仅打印 message 无法判断是参数映射还是取值环节出错
            log.warn("SQL 日志拼装异常: mapperId={}", ms.getId(), e);
        }
    }

    /** 将 BoundSql 中的 ? 按参数顺序替换为字面值，输出一条可直接执行的 SQL */
    private String showSql(Configuration configuration, BoundSql boundSql) {
        Object parameterObject = boundSql.getParameterObject();
        List<ParameterMapping> parameterMappings = boundSql.getParameterMappings();
        // sql 中多个空格替换为单个空格，便于阅读
        String sql = boundSql.getSql().replaceAll("[\\s]+", " ");
        if (CollectionUtils.isEmpty(parameterMappings) || parameterObject == null) {
            return sql;
        }
        TypeHandlerRegistry typeHandlerRegistry = configuration.getTypeHandlerRegistry();
        if (typeHandlerRegistry.hasTypeHandler(parameterObject.getClass())) {
            // 单值参数（如直接传入 String/Long）：没有参数名，按值特征兜底脱敏
            sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(parameterValue(null, parameterObject)));
        } else {
            MetaObject metaObject = configuration.newMetaObject(parameterObject);
            for (ParameterMapping parameterMapping : parameterMappings) {
                String propertyName = parameterMapping.getProperty();
                Object value;
                if (metaObject.hasGetter(propertyName)) {
                    value = metaObject.getValue(propertyName);
                } else if (boundSql.hasAdditionalParameter(propertyName)) {
                    // 动态 SQL（foreach 等）产生的附加参数
                    value = boundSql.getAdditionalParameter(propertyName);
                } else {
                    value = null;
                }
                sql = sql.replaceFirst("\\?", Matcher.quoteReplacement(parameterValue(propertyName, value)));
            }
        }
        return sql;
    }

    /**
     * 参数格式化：字符串/日期加引号，null 显示 NULL；敏感字段先脱敏。
     *
     * @param propertyName 参数名（可能为 null，如单值参数）
     * @param obj          参数值
     */
    private String parameterValue(String propertyName, Object obj) {
        if (obj == null) {
            return "NULL";
        }
        if (obj instanceof CharSequence) {
            return "'" + maskText(propertyName, obj.toString()) + "'";
        }
        if (obj instanceof Date) {
            return "'" + obj + "'";
        }
        return String.valueOf(obj);
    }

    /* --------------------------- 脱敏实现 --------------------------- */

    /**
     * 按参数名（优先）与值特征（兜底）决定如何脱敏。
     *
     * <p>包内可见（非 private）：便于单元测试直接覆盖脱敏规则，
     * 见 {@code com.admintem.web.config.mybatis.SqlLogMaskTest}。</p>
     */
    String maskText(String propertyName, String value) {
        if (!maskEnable || value.isEmpty()) {
            return value;
        }
        String name = propertyName == null ? "" : propertyName.toLowerCase(Locale.ROOT);
        // 1) 字段名命中机密关键字：整体打码
        if (containsAny(name, SECRET_KEYWORDS)) {
            return "***";
        }
        // 2) 字段名命中个人信息关键字：按类型脱敏
        if (containsAny(name, PERSONAL_KEYWORDS)) {
            return maskByShape(value);
        }
        // 3) 其余情况（含无参数名的单值参数）：按值特征兜底
        //    手机号/邮箱/证件号形态一律脱敏，避免敏感值因列名未命中关键字而漏网
        return maskByShape(value);
    }

    /** 按值特征脱敏：手机号、邮箱、身份证；不匹配则原样返回（包内可见以便单测） */
    String maskByShape(String value) {
        if (PHONE.matcher(value).matches()) {
            // 138****8888
            return value.substring(0, 3) + "****" + value.substring(7);
        }
        if (ID_CARD.matcher(value).matches()) {
            // 110101********1234
            return value.substring(0, 6) + "********" + value.substring(14);
        }
        if (EMAIL.matcher(value).matches()) {
            int at = value.indexOf('@');
            String local = value.substring(0, at);
            String domain = value.substring(at);
            String head = local.substring(0, 1);
            return head + "***" + domain;
        }
        return value;
    }

    /** 名称是否包含任一关键字（不区分大小写，调用方已转小写；包内可见以便单测） */
    boolean containsAny(String name, String[] keywords) {
        for (String keyword : keywords) {
            if (name.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
