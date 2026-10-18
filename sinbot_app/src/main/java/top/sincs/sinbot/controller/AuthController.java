package top.sincs.sinbot.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import top.sincs.sinbot.common.Result;
import top.sincs.sinbot.dto.auth.LoginDTO;
import top.sincs.sinbot.security.LoginUser;
import top.sincs.sinbot.security.SecurityUtils;
import top.sincs.sinbot.service.AuthService;
import top.sincs.sinbot.vo.auth.LoginVO;

/**
 * 认证入口。
 *
 * <p>登录成功后客户端需在后续请求中以
 * {@code Authorization: Bearer <token>} 携带凭证。</p>
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /** 登录，成功返回 token */
    @PostMapping("/login")
    public LoginVO login(@RequestBody @Valid LoginDTO dto) {
        return authService.login(dto);
    }

    /** 登出当前 token（其它设备的登录态不受影响） */
    @PostMapping("/logout")
    public Result<Void> logout() {
        authService.logout();
        return Result.success();
    }

    /** 当前登录用户 */
    @GetMapping("/me")
    public LoginUser me() {
        return SecurityUtils.getLoginUser();
    }
}
