package top.sincs.sinbot.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
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
 * 安全配置：无状态 Header Token 认证，一套接口同时服务 App 与 Web UI。
 *
 * <p>连接方式：客户端登录后拿到 token，后续请求以
 * {@code Authorization: Bearer <token>} 携带。</p>
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

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
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            RobotAuthenticationFilter robotAuthenticationFilter,
            TokenAuthenticationFilter tokenAuthenticationFilter,
            RestAuthenticationEntryPoint restAuthenticationEntryPoint,
            RestAccessDeniedHandler restAccessDeniedHandler) throws Exception {

        http
                // ==================== CSRF（保留配置，当前关闭） ====================
                .csrf(csrf -> {
                    if (!csrfEnabled) {
                        csrf.disable();
                        return;
                    }
                    // 将来开启时（Web UI 使用 Cookie 会话）：
                    //   1) token 写入 XSRF-TOKEN cookie，前端取出回填 X-XSRF-TOKEN 头
                    //   2) App 走的路径不需要 CSRF，按路径豁免
                    // csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    //     .ignoringRequestMatchers("/app/**");
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
                        // Druid 监控页由它自己的 admin/admin 保护，生产环境请加 IP 白名单
                        .requestMatchers("/druid/**", "/error", "/actuator/health").permitAll()
                        .anyRequest().authenticated())

                // 两条认证通道：先认机器人的 X-Api-Key，未命中才落到 Bearer token（DR-03）
                .addFilterBefore(robotAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
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
