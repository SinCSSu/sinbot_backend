package top.sincs.sinbot.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import top.sincs.sinbot.entity.SysUser;
import top.sincs.sinbot.repository.SysUserMapper;

/**
 * 首次启动时创建初始账号（仅当 {@code sys_user} 表为空）。
 *
 * <p>生产环境请设置 {@code sinbot.security.init-user.enabled=false}，
 * 并在首次登录后立即修改密码。</p>
 */
@Slf4j
@Component
@ConditionalOnProperty(name = "sinbot.security.init-user.enabled", havingValue = "true", matchIfMissing = true)
public class SysUserInitializer implements ApplicationRunner {

    private final SysUserMapper sysUserMapper;

    private final PasswordEncoder passwordEncoder;

    @Value("${sinbot.security.init-user.username:admin}")
    private String username;

    @Value("${sinbot.security.init-user.password:admin123}")
    private String password;

    public SysUserInitializer(SysUserMapper sysUserMapper, PasswordEncoder passwordEncoder) {
        this.sysUserMapper = sysUserMapper;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            Long count = sysUserMapper.selectCount(null);
            if (count != null && count > 0) {
                return;
            }
            SysUser user = new SysUser();
            user.setUsername(username);
            user.setPassword(passwordEncoder.encode(password));
            user.setNickname("初始管理员");
            user.setRole("ADMIN");
            user.setStatus(SysUser.STATUS_ENABLED);
            sysUserMapper.insert(user);
            log.warn("sys_user 表为空，已创建初始账号 [{}]，密码取自 sinbot.security.init-user.password，请尽快修改",
                    username);
        } catch (Exception e) {
            log.error("初始账号创建失败，请确认已执行 sys_user 建表脚本: {}", e.getMessage());
        }
    }
}
