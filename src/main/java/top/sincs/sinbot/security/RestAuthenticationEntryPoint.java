package top.sincs.sinbot.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import top.sincs.sinbot.common.ErrorCode;
import top.sincs.sinbot.common.Result;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * 未认证（401）时的响应处理。
 *
 * <p>认证失败发生在过滤器链中，不会进入 DispatcherServlet，
 * 因此 {@code ResponseBodyAdvice} 无法覆盖，需要在此手动输出统一响应壳。</p>
 *
 * <p>接入方式（在你的 SecurityFilterChain 配置中）：</p>
 * <pre>{@code
 * http.exceptionHandling(ex -> ex.authenticationEntryPoint(restAuthenticationEntryPoint));
 * }</pre>
 */
@Component
public class RestAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    public RestAuthenticationEntryPoint(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void commence(HttpServletRequest request,
                         HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        // 携带了凭证但已失效（过期 / 已登出 / 被踢下线）时，给出更精确的错误码
        Object invalid = request.getAttribute(TokenAuthenticationFilter.ATTR_INVALID_TOKEN);
        writeResult(response, invalid != null ? ErrorCode.TOKEN_INVALID : ErrorCode.UNAUTHORIZED);
    }

    public void writeResult(HttpServletResponse response, ErrorCode errorCode) throws IOException {
        // 与业务错误保持一致：HTTP 状态码仍为 200，业务码通过 Result.code 体现
        response.setStatus(HttpServletResponse.SC_OK);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getOutputStream(), Result.failure(errorCode));
    }
}
