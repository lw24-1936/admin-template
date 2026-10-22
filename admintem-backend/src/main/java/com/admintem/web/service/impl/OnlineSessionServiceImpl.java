package com.admintem.web.service.impl;

import com.admintem.web.config.AdmintemProperties;
import com.admintem.web.entity.SysAuthRole;
import com.admintem.web.entity.SysAuthUser;
import com.admintem.web.entity.SysAuthUserRole;
import com.admintem.web.entity.vo.OnlineSessionVO;
import com.admintem.web.mapper.SysAuthRoleMapper;
import com.admintem.web.mapper.SysAuthUserMapper;
import com.admintem.web.mapper.SysAuthUserRoleMapper;
import com.admintem.web.security.JwtTokenService;
import com.admintem.web.service.OnlineSessionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBucket;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 在线会话实现：直接读 Redis 令牌白名单。
 *
 * <p><b>为什么不做成数据库表：</b>令牌白名单本来就是"谁有有效令牌"的唯一事实来源。
 * 再建一张在线表就出现两处状态，还要处理"应用崩溃后表里残留脏数据"。
 * 读 Redis 的代价是一次 SCAN + N 次 GET（在线人数通常是个位数），足够。</p>
 *
 * <p><b>为什么限制 200 条：</b>SCAN 在极端情况（被刷出大量 key）下可能返回很多，
 * 接口必须有自己的上限，避免一次请求把响应撑爆。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class OnlineSessionServiceImpl implements OnlineSessionService {

    /** 单次返回的最大会话数（防御性上限） */
    private static final int MAX_SESSIONS = 200;

    /** 令牌摘要前缀长度（只用于区分，不泄露完整令牌） */
    private static final int DIGEST_LENGTH = 8;

    private final RedissonClient redissonClient;
    private final JwtTokenService jwtTokenService;
    private final SysAuthUserMapper userMapper;
    private final SysAuthUserRoleMapper userRoleMapper;
    private final SysAuthRoleMapper roleMapper;
    private final AdmintemProperties properties;

    @Override
    public List<OnlineSessionVO> listSessions(String currentToken) {
        String prefix = properties.getJwt().getRedisKeyPrefix();
        List<String> keys = new ArrayList<>();
        for (String key : redissonClient.getKeys().getKeysByPattern(prefix + "*")) {
            keys.add(key);
            if (keys.size() >= MAX_SESSIONS) {
                log.warn("在线会话数量达到上限 {}，本次只返回前 {} 条", MAX_SESSIONS, MAX_SESSIONS);
                break;
            }
        }
        if (keys.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. 一次 MGET 取回全部令牌值；TTL 没有批量 API，仍按 key 读（200 条上限内）
        Map<String, String> values = redissonClient.getBuckets().get(keys.toArray(new String[0]));
        Map<String, String> tokenByUser = new HashMap<>();
        Map<String, Long> ttlByUser = new HashMap<>();
        for (String key : keys) {
            String userName = key.substring(prefix.length());
            String token = values == null ? null : values.get(key);
            if (token == null) {
                // 刚好过期/被删：跳过，避免把"已失效"显示成"在线"
                continue;
            }
            long ttl = redissonClient.getBucket(key).remainTimeToLive();
            if (ttl <= 0) {
                continue;
            }
            tokenByUser.put(userName, token);
            ttlByUser.put(userName, ttl / 1000);
        }
        if (tokenByUser.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. 批量补用户信息（一次查询，避免逐个 selectByUserName 的 N+1）
        List<SysAuthUser> users = userMapper.selectByUserNames(tokenByUser.keySet());
        Map<String, SysAuthUser> userByName = users == null ? Collections.emptyMap()
                : users.stream().collect(Collectors.toMap(SysAuthUser::getUserName, u -> u, (a, b) -> a));

        // 3. 批量补角色名（一次查关联 + 一次查角色）
        Map<Long, String> roleNamesByUser = loadRoleNames(users);

        // 4. 组装并按剩余有效期倒序
        List<OnlineSessionVO> list = new ArrayList<>(tokenByUser.size());
        for (Map.Entry<String, String> entry : tokenByUser.entrySet()) {
            String userName = entry.getKey();
            String token = entry.getValue();
            SysAuthUser user = userByName.get(userName);

            OnlineSessionVO vo = new OnlineSessionVO();
            vo.setUserName(userName);
            vo.setNickName(user == null ? null : user.getNickName());
            vo.setStatus(user == null ? null : user.getStatus());
            vo.setRoleNames(user == null ? null : roleNamesByUser.get(user.getId()));
            vo.setExpireSeconds(ttlByUser.get(userName));
            vo.setTokenDigest(digest(token));
            vo.setCurrent(currentToken != null && token.equals(currentToken));
            vo.setLocked(isLocked(userName));
            list.add(vo);
        }
        list.sort(Comparator.comparing(OnlineSessionVO::getExpireSeconds,
                Comparator.nullsLast(Comparator.reverseOrder())));
        return list;
    }

    @Override
    public boolean kick(String userName) {
        // 复用退出登录的同一条路径：删除白名单 key；不额外清理权限快照，
        // 因为快照只影响"已登录请求"的性能，令牌失效后它自然过期
        boolean existed = isOnline(userName);
        jwtTokenService.revokeToken(userName);
        log.info("强制下线: userName={}, 原会话存在={}", userName, existed);
        return existed;
    }

    /** 账号是否有有效令牌（TTL > 0） */
    private boolean isOnline(String userName) {
        RBucket<String> bucket = redissonClient.getBucket(properties.getJwt().getRedisKeyPrefix() + userName);
        return bucket.get() != null && bucket.remainTimeToLive() > 0;
    }

    /** 是否处于登录锁定状态（锁定中的账号即便持有令牌，登录接口也会拒绝，但已登录令牌仍有效） */
    private boolean isLocked(String userName) {
        try {
            return redissonClient.getBucket(properties.getAuth().getLockKeyPrefix() + userName).isExists();
        } catch (Exception e) {
            // 只是展示信息，读失败不应让整页报错
            log.warn("读取登录锁定状态失败: userName={}, err={}", userName, e.getMessage());
            return false;
        }
    }

    /**
     * 批量查询各用户的角色名。
     *
     * @param users 用户列表（可为空）
     * @return userId → "管理员,测试角色1"
     */
    private Map<Long, String> loadRoleNames(List<SysAuthUser> users) {
        if (users == null || users.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> userIds = users.stream().map(SysAuthUser::getId).filter(Objects::nonNull).toList();
        List<SysAuthUserRole> links = userRoleMapper.selectByUserIds(userIds);
        if (links == null || links.isEmpty()) {
            return Collections.emptyMap();
        }
        Set<Long> roleIds = links.stream().map(SysAuthUserRole::getRoleId).filter(Objects::nonNull)
                .collect(Collectors.toSet());
        List<SysAuthRole> roles = roleIds.isEmpty() ? Collections.emptyList() : roleMapper.selectByIds(roleIds);
        Map<Long, String> roleNameById = roles.stream()
                .collect(Collectors.toMap(SysAuthRole::getId, SysAuthRole::getRoleName, (a, b) -> a));

        Map<Long, List<String>> namesByUser = new HashMap<>();
        for (SysAuthUserRole link : links) {
            String name = roleNameById.get(link.getRoleId());
            if (name != null) {
                namesByUser.computeIfAbsent(link.getUserId(), k -> new ArrayList<>()).add(name);
            }
        }
        Map<Long, String> result = new HashMap<>();
        namesByUser.forEach((userId, names) -> result.put(userId, String.join(",", names)));
        return result;
    }

    /** 令牌摘要：前 8 位 + 省略号，足够区分同一账号的多条会话 */
    private String digest(String token) {
        if (token == null) {
            return null;
        }
        return token.length() <= DIGEST_LENGTH ? token : token.substring(0, DIGEST_LENGTH) + "…";
    }
}
