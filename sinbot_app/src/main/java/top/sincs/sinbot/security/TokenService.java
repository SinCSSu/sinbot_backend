package top.sincs.sinbot.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.Base64;
import java.util.Set;

/**
 * 不透明 token 的签发、校验、续期与撤销。
 *
 * <p>Redis 是登录态的唯一权威存储，token 本身不携带任何信息，
 * 因此服务端可以随时作废（这是相比 JWT 的核心优势）。</p>
 *
 * <p>Key 设计：</p>
 * <ul>
 *     <li>{@code sinbot:auth:token:{token}} → String，登录态 JSON，TTL 为滑动过期时长</li>
 *     <li>{@code sinbot:auth:user:{userId}} → ZSet，member 为 token、score 为签发时间，用于一键下线</li>
 * </ul>
 */
@Slf4j
@Service
public class TokenService {

    /** token 详情前缀 */
    private static final String TOKEN_KEY_PREFIX = "sinbot:auth:token:";

    /** 用户 token 集合前缀 */
    private static final String USER_TOKEN_KEY_PREFIX = "sinbot:auth:user:";

    private final StringRedisTemplate redis;

    private final ObjectMapper objectMapper;

    private final SecureRandom secureRandom = new SecureRandom();

    /** 滑动过期时长（分钟）：每次有效请求都会续期 */
    @Value("${sinbot.security.token.ttl-minutes:120}")
    private long ttlMinutes;

    /** 绝对过期上限（分钟）：超过后不再续期，必须重新登录 */
    @Value("${sinbot.security.token.max-ttl-minutes:10080}")
    private long maxTtlMinutes;

    /** 单用户并发登录数上限，超出后淘汰最旧的一个 */
    @Value("${sinbot.security.token.max-per-user:5}")
    private int maxTokensPerUser;

    public TokenService(StringRedisTemplate redis, ObjectMapper objectMapper) {
        this.redis = redis;
        this.objectMapper = objectMapper;
    }

    /** 对外返回的相对过期时间（秒） */
    public long getTtlSeconds() {
        return Duration.ofMinutes(ttlMinutes).toSeconds();
    }

    /** 签发 token 并写入 Redis */
    public String create(LoginUser loginUser) {
        String token = randomToken();
        loginUser.setLoginTime(System.currentTimeMillis());

        redis.opsForValue().set(TOKEN_KEY_PREFIX + token,
                objectMapper.writeValueAsString(loginUser),
                Duration.ofMinutes(ttlMinutes));

        String userKey = USER_TOKEN_KEY_PREFIX + loginUser.getUserId();
        redis.opsForZSet().add(userKey, token, loginUser.getLoginTime());
        redis.expire(userKey, Duration.ofMinutes(maxTtlMinutes));

        evictOldestIfExceeded(userKey);
        return token;
    }

    /**
     * 校验 token 并滑动续期。
     *
     * @return 命中且未超过绝对上限时返回登录态，否则返回 {@code null}
     */
    public LoginUser parse(String token) {
        String key = TOKEN_KEY_PREFIX + token;
        String json = redis.opsForValue().get(key);
        if (json == null) {
            return null;
        }
        LoginUser loginUser = objectMapper.readValue(json, LoginUser.class);

        long elapsedMinutes = (System.currentTimeMillis() - loginUser.getLoginTime()) / 60_000L;
        if (elapsedMinutes < maxTtlMinutes) {
            redis.expire(key, Duration.ofMinutes(ttlMinutes));
        }
        return loginUser;
    }

    /** 登出：只失效当前 token，不影响该用户其它设备的登录态 */
    public void revoke(String token, Long userId) {
        if (token == null) {
            return;
        }
        redis.delete(TOKEN_KEY_PREFIX + token);
        if (userId != null) {
            redis.opsForZSet().remove(USER_TOKEN_KEY_PREFIX + userId, token);
        }
    }

    /** 一键下线：清空该用户全部设备的登录态 */
    public void revokeAll(Long userId) {
        String userKey = USER_TOKEN_KEY_PREFIX + userId;
        Set<String> tokens = redis.opsForZSet().range(userKey, 0, -1);
        if (tokens != null && !tokens.isEmpty()) {
            redis.delete(tokens.stream().map(TOKEN_KEY_PREFIX::concat).toList());
        }
        redis.delete(userKey);
    }

    /** 32 字节安全随机数，Base64 URL-safe 编码后为 43 个字符 */
    private String randomToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** 同一用户登录设备数超限时，淘汰签发时间最早的一个 */
    private void evictOldestIfExceeded(String userKey) {
        Long size = redis.opsForZSet().size(userKey);
        if (size == null || size <= maxTokensPerUser) {
            return;
        }
        Set<String> oldest = redis.opsForZSet().range(userKey, 0, size - maxTokensPerUser - 1);
        if (oldest == null || oldest.isEmpty()) {
            return;
        }
        redis.delete(oldest.stream().map(TOKEN_KEY_PREFIX::concat).toList());
        redis.opsForZSet().remove(userKey, oldest.toArray());
    }
}
