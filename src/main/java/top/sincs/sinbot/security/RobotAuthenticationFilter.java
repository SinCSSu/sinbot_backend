package top.sincs.sinbot.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import top.sincs.sinbot.config.RobotProperties;
import top.sincs.sinbot.constant.HeaderConstant;

import java.io.IOException;
import java.util.List;

/**
 * 机器人认证明通道：校验 {@code X-Api-Key}，并把 {@code X-Operator-Qq} 作为当前身份（DR-03 / DR-08）。
 *
 * <p>机器人与管理 App 走的是完全不同的凭据，因此这里把它做成一条独立通道，
 * 插在 {@link TokenAuthenticationFilter} 之前：带 {@code X-Api-Key} 的请求不再走 Bearer 解析。</p>
 *
 * <p>身份建模的关键点：principal 仍是 {@link LoginUser}，但 {@code username} 填的是请求头里的操作人 QQ。
 * 这样业务侧{@code SecurityUtils.getLoginUser().getUsername()} 在两条链路上取到的都是 QQ 号（DR-07），
 * 不需要任何「按来源分支」的判断代码。</p>
 *
 * <p>校验失败不抛异常、也不直接写响应，交由 {@link RestAuthenticationEntryPoint} 统一出口。</p>
 */
@Component
@RequiredArgsConstructor
public class RobotAuthenticationFilter extends OncePerRequestFilter {

    /** 携带了 key 但无效时置入该属性，供入口点区分错误码 */
    public static final String ATTR_INVALID_ROBOT_KEY = "sinbot.invalid-robot-key";

    public static final String ROLE_ROBOT = "ROLE_ROBOT";

    private final RobotProperties robotProperties;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String apiKey = request.getHeader(HeaderConstant.API_KEY);
        if (!StringUtils.hasText(apiKey)) {
            // 不是机器人请求，交给 Token 通道
            filterChain.doFilter(request, response);
            return;
        }

        if (!robotProperties.matches(apiKey.trim())) {
            request.setAttribute(ATTR_INVALID_ROBOT_KEY, Boolean.TRUE);
            filterChain.doFilter(request, response);
            return;
        }

        LoginUser robot = new LoginUser();
        // DR-08：操作人不由调用方在 body 里自称，一律取自请求头。
        // 头缺失时这里不拦——只读接口（按群查团队等）用不到操作人；写接口一律在 controller 层
        // 由 SecurityUtils.requireOperatorQq() 拒绝，全仓只有这一个取值入口。
        robot.setUsername(request.getHeader(HeaderConstant.OPERATOR_QQ));
        robot.setNickname("robot");
        robot.setRoles(List.of(ROLE_ROBOT));
        robot.setLoginTime(System.currentTimeMillis());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(robot, null,
                        List.of(new SimpleGrantedAuthority(ROLE_ROBOT))));

        filterChain.doFilter(request, response);
    }
}
