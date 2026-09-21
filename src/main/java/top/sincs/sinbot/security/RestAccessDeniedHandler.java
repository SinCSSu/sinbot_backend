package top.sincs.sinbot.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;
import top.sincs.sinbot.common.ErrorCode;

import java.io.IOException;

/**
 * 已认证但无权限（403）时的响应处理，同样输出统一响应壳。
 *
 * <p>接入方式（在你的 SecurityFilterChain 配置中）：</p>
 * <pre>{@code
 * http.exceptionHandling(ex -> ex.accessDeniedHandler(restAccessDeniedHandler));
 * }</pre>
 */
@Component
public class RestAccessDeniedHandler implements AccessDeniedHandler {

    private final RestAuthenticationEntryPoint entryPoint;

    public RestAccessDeniedHandler(RestAuthenticationEntryPoint entryPoint) {
        this.entryPoint = entryPoint;
    }

    @Override
    public void handle(HttpServletRequest request,
                       HttpServletResponse response,
                       AccessDeniedException accessDeniedException) throws IOException {
        entryPoint.writeResult(response, ErrorCode.FORBIDDEN);
    }
}
