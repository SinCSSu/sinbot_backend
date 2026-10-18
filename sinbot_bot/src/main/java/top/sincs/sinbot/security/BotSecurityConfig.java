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
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.RequestHeaderRequestMatcher;
import top.sincs.sinbot.constant.HeaderConstant;

/**
 * 机器人侧安全配置：只认 {@code X-Api-Key}，与 App 的 Bearer token 通道互不干扰。
 *
 * <p>拆分前两条认证通道串在同一条 {@code SecurityFilterChain} 上，靠先后顺序短路。
 * 拆分后机器人与 App 分属两个模块，各自装配自己的链，这里用
 * {@code securityMatcher} 按 {@code X-Api-Key} 请求头分流：带该头的请求进本链，
 * 其余请求落到 {@code AppSecurityConfig} 的兜底链。</p>
 *
 * <p>本链 {@code @Order(1)} 必须排在 App 链之前，否则会被无 matcher 的兜底链抢先匹配。</p>
 */
@Configuration
@EnableWebSecurity
public class BotSecurityConfig {

    /**
     * CSRF 开关。
     *
     * <p>当前为 {@code false}：机器人在 header 中显式携带凭证，
     * 不存在"浏览器自动附带身份"这一前提，CSRF 攻击不成立。</p>
     */
    @Value("${sinbot.security.csrf-enabled:false}")
    private boolean csrfEnabled;

    @Bean
    @Order(1)
    public SecurityFilterChain botSecurityFilterChain(
            HttpSecurity http,
            RobotAuthenticationFilter robotAuthenticationFilter,
            RestAuthenticationEntryPoint restAuthenticationEntryPoint,
            RestAccessDeniedHandler restAccessDeniedHandler) throws Exception {

        http
                // 只有带 X-Api-Key 头的请求才进本链，其余交给 App 链
                .securityMatcher(new RequestHeaderRequestMatcher(HeaderConstant.API_KEY))

                // ==================== CSRF（保留配置，当前关闭） ====================
                .csrf(csrf -> {
                    if (!csrfEnabled) {
                        csrf.disable();
                    }
                })

                // ==================== CORS ====================
                .cors(Customizer.withDefaults())

                // 无状态：不创建 session，杜绝 JSESSIONID 被客户端自动回传
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                // 关闭默认的表单登录与 Basic
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/auth/login").permitAll()
                        // 健康检查探针：容器 / 负载均衡不带凭证访问，必须放行
                        .requestMatchers("/actuator/health", "/actuator/health/**", "/livez", "/readyz").permitAll()
                        // Druid 监控页由它自己的 admin/admin 保护，生产环境请加 IP 白名单
                        .requestMatchers("/druid/**", "/error").permitAll()
                        .anyRequest().authenticated())

                .addFilterBefore(robotAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)

                // 401 / 403 统一输出响应壳
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .accessDeniedHandler(restAccessDeniedHandler));

        return http.build();
    }
}
