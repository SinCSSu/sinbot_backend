package top.sincs.sinbot.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * App 管理端安全配置：只认 {@code Authorization: Bearer <token>}，token 来自 {@code /auth/login}。
 *
 * <p>本链不设 {@code securityMatcher}，作为兜底链承接所有未命中机器人链的请求
 * （即所有不带 {@code X-Api-Key} 头的请求），因此 {@code @Order(2)} 必须排在
 * {@code BotSecurityConfig} 之后。</p>
 *
 * <p>{@code PasswordEncoder} 放在这里，因为它的两个使用方——{@code AuthServiceImpl}
 * 与 {@code SysUserInitializer}——都属于 App 侧的账号体系。</p>
 */
@Configuration
@EnableWebSecurity
public class AppSecurityConfig {

    /**
     * CSRF 开关。
     *
     * <p>当前为 {@code false}：客户端在 header 中显式携带凭证，
     * 不存在"浏览器自动附带身份"这一前提，CSRF 攻击不成立。
     * 将来 Web UI 若改用 Cookie 会话，置为 true 并打开下方分支即可。</p>
     */
    @Value("${sinbot.security.csrf-enabled:false}")
    private boolean csrfEnabled;

    @Bean
    @Order(2)
    public SecurityFilterChain appSecurityFilterChain(
            HttpSecurity http,
            TokenAuthenticationFilter tokenAuthenticationFilter,
            RestAuthenticationEntryPoint restAuthenticationEntryPoint,
            RestAccessDeniedHandler restAccessDeniedHandler) throws Exception {

        http
                // ==================== CSRF（保留配置，当前关闭） ====================
                .csrf(csrf -> {
                    if (!csrfEnabled) {
                        csrf.disable();
                    }
                })

                // ==================== CORS（服务 Web UI；原生 App 不受 CORS 约束） ====================
                .cors(Customizer.withDefaults())

                // 无状态：不创建 session，杜绝 JSESSIONID 被客户端自动回传
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 关闭默认的表单登录与 Basic，统一由 TokenAuthenticationFilter 认证
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(auth -> auth
                        // 登录接口必须匿名可访问
                        .requestMatchers("/auth/login").permitAll()
                        // 健康检查探针：容器 / 负载均衡不带凭证访问，必须放行
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/livez", "/readyz").permitAll()
                        // Druid 监控页由它自己的 admin/admin 保护，生产环境请加 IP 白名单
                        .requestMatchers("/druid/**", "/error").permitAll()
                        .anyRequest().authenticated())

                .addFilterBefore(tokenAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // 401 / 403 统一输出响应壳
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler));

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
