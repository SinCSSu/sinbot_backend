package top.sincs.sinbot.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * 解析 {@code Authorization: Bearer <token>} 并写入 {@code SecurityContext}。
 *
 * <p>解析失败不抛异常、也不直接写响应：认证结果交由后续的
 * {@code AuthorizationFilter} 判定，再由 {@link RestAuthenticationEntryPoint}
 * 统一输出响应壳，保证错误出口唯一。</p>
 */
@Component
public class TokenAuthenticationFilter extends OncePerRequestFilter {

    public static final String BEARER_PREFIX = "Bearer ";

    /** 携带了凭证但已失效时置入该属性，供入口点区分错误码 */
    public static final String ATTR_INVALID_TOKEN = "sinbot.invalid-token";

    private final TokenService tokenService;

    public TokenAuthenticationFilter(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
            String token = header.substring(BEARER_PREFIX.length()).trim();
            LoginUser loginUser = tokenService.parse(token);
            if (loginUser == null) {
                request.setAttribute(ATTR_INVALID_TOKEN, Boolean.TRUE);
            } else {
                loginUser.setToken(token);
                SecurityContextHolder.getContext().setAuthentication(
                        new UsernamePasswordAuthenticationToken(loginUser, null, authoritiesOf(loginUser)));
            }
        }
        filterChain.doFilter(request, response);
    }

    private List<SimpleGrantedAuthority> authoritiesOf(LoginUser loginUser) {
        return loginUser.getRoles() == null
                ? List.of()
                : loginUser.getRoles().stream().map(SimpleGrantedAuthority::new).toList();
    }
}
