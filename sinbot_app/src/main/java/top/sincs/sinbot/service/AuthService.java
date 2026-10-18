package top.sincs.sinbot.service;

import top.sincs.sinbot.dto.auth.LoginDTO;
import top.sincs.sinbot.vo.auth.LoginVO;

/** 认证服务。 */
public interface AuthService {

    /** 校验账号密码并签发 token */
    LoginVO login(LoginDTO dto);

    /** 登出：仅失效当前请求携带的 token */
    void logout();
}
