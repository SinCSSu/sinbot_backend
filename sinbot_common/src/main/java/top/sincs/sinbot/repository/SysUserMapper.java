package top.sincs.sinbot.repository;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import top.sincs.sinbot.entity.SysUser;

/**
 * 登录账号持久层。
 *
 * <p>由 {@code MybatisPlusConfig} 上的 {@code @MapperScan("top.sincs.sinbot.repository")} 注册，
 * 无需额外注解。</p>
 */
public interface SysUserMapper extends BaseMapper<SysUser> {
}
