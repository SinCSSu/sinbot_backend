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
import top.sincs.sinbot.constant.SecurityAttrConstant;

import java.io.IOException;
import java.util.List;

/**
 * 机器人认证明通道：校验 {@code X-Api-Key}，并把 {@code X-Operator-Qq} / {@code X-Group-Number}
 * 作为当前身份（DR-03 / DR-04 / DR-08）。
 *
 * <p>机器人与管理 App 走的是完全不同的凭据，因此这里把它做成一条独立通道：
 * 带 {@code X-Api-Key} 的请求走机器人链，其余请求落到 App 的 Bearer token 链。</p>
 *
 * <p>身份建模的关键点：principal 仍是 {@link LoginUser}，但 {@code username} 填的是请求头里的操作人 QQ。
 * 这样业务侧{@code SecurityUtils.getLoginUser().getUsername()} 在两条链路上取到的都是 QQ 号（DR-07），
 * 不需要任何「按来源分支」的判断代码。</p>
 *
 * <p>群号与操作人都在这里一次性校验并写入身份上下文：二者同属「机器人声明的上下文」而非业务参数，
 * 分散到每个 controller 上去 {@code @RequestHeader} 只会让「哪个接口漏校验」成为长期隐患，
 * 且缺头时会退化成缺参异常（50000）而非明确的 10010 / 10011。收口之后，
 * controller 只需 {@code SecurityUtils.requireGroupNumber()} 取值，方法签名里不再出现这两个参数。</p>
 *
 * <p>校验失败不抛异常、也不直接写响应，交由 {@link RestAuthenticationEntryPoint} 统一出口。</p>
 */
@Component
@RequiredArgsConstructor
public class RobotAuthenticationFilter extends OncePerRequestFilter {

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
            request.setAttribute(SecurityAttrConstant.ATTR_INVALID_ROBOT_KEY, Boolean.TRUE);
            filterChain.doFilter(request, response);
            return;
        }

        // DR-04：群号是分区键，缺了它所有按群查询都会失焦，前置拒绝
        String groupNumber = headerValue(request, HeaderConstant.GROUP_NUMBER);
        if (groupNumber == null) {
            request.setAttribute(SecurityAttrConstant.ATTR_MISSING_GROUP_NUMBER, Boolean.TRUE);
            filterChain.doFilter(request, response);
            return;
        }

        // DR-08：操作人不由调用方在 body 里自称，一律取自请求头。
        // X-Api-Key 只证明「请求来自机器人」，「谁在群里发指令」另有其人，且这个值没有任何旁证可推导。
        String operatorQq = headerValue(request, HeaderConstant.OPERATOR_QQ);
        if (operatorQq == null) {
            request.setAttribute(SecurityAttrConstant.ATTR_MISSING_OPERATOR_QQ, Boolean.TRUE);
            filterChain.doFilter(request, response);
            return;
        }

        LoginUser robot = new LoginUser();
        robot.setUsername(operatorQq);
        robot.setGroupNumber(groupNumber);
        robot.setNickname("robot");
        robot.setRoles(List.of(SecurityAttrConstant.ROLE_ROBOT));
        robot.setLoginTime(System.currentTimeMillis());

        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(robot, null,
                        List.of(new SimpleGrantedAuthority(SecurityAttrConstant.ROLE_ROBOT))));

        filterChain.doFilter(request, response);
    }

    /** 取请求头并去空白，缺失或全空白返回 {@code null} */
    private static String headerValue(HttpServletRequest request, String headerName) {
        String value = request.getHeader(headerName);
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
