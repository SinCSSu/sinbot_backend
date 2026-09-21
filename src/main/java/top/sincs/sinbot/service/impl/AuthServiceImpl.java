package top.sincs.sinbot.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import top.sincs.sinbot.common.ErrorCode;
import top.sincs.sinbot.dto.auth.LoginDTO;
import top.sincs.sinbot.entity.SysUser;
import top.sincs.sinbot.exception.BusinessException;
import top.sincs.sinbot.repository.SysUserMapper;
import top.sincs.sinbot.security.LoginUser;
import top.sincs.sinbot.security.SecurityUtils;
import top.sincs.sinbot.security.TokenService;
import top.sincs.sinbot.service.AuthService;
import top.sincs.sinbot.vo.auth.LoginVO;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final SysUserMapper sysUserMapper;

    private final PasswordEncoder passwordEncoder;

    private final TokenService tokenService;

    @Override
    public LoginVO login(LoginDTO dto) {
        SysUser user = sysUserMapper.selectOne(Wrappers.<SysUser>lambdaQuery()
                .eq(SysUser::getUsername, dto.getUsername()));

        // 账号不存在与密码错误返回同一文案，避免被用来枚举账号
        if (user == null || !passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            log.warn("登录失败，账号或密码错误: {}", dto.getUsername());
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        if (user.getStatus() == null || user.getStatus() != SysUser.STATUS_ENABLED) {
            log.warn("登录失败，账号已停用: {}", dto.getUsername());
            throw new BusinessException(ErrorCode.ACCOUNT_DISABLED);
        }

        LoginUser loginUser = new LoginUser();
        loginUser.setUserId(user.getUserId());
        loginUser.setUsername(user.getUsername());
        loginUser.setNickname(user.getNickname());
        loginUser.setRoles(List.of(roleAuthority(user.getRole())));

        String token = tokenService.create(loginUser);
        log.info("登录成功: {} (userId={})", user.getUsername(), user.getUserId());

        return new LoginVO()
                .setToken(token)
                .setExpiresIn(tokenService.getTtlSeconds())
                .setUserId(user.getUserId())
                .setUsername(user.getUsername())
                .setNickname(user.getNickname())
                .setRole(user.getRole());
    }

    @Override
    public void logout() {
        LoginUser loginUser = SecurityUtils.getLoginUser();
        tokenService.revoke(loginUser.getToken(), loginUser.getUserId());
        log.info("登出: {} (userId={})", loginUser.getUsername(), loginUser.getUserId());
    }

    /** 落库的角色标识转为 Spring Security 的 authority 形式 */
    private String roleAuthority(String role) {
        if (role == null || role.isBlank()) {
            return "ROLE_USER";
        }
        return role.startsWith("ROLE_") ? role : "ROLE_" + role;
    }
}
