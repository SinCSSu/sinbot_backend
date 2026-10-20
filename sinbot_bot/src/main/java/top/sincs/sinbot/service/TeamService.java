package top.sincs.sinbot.service;

import top.sincs.sinbot.vo.team.TeamDetailVo;
import top.sincs.sinbot.vo.team.TeamInfoVo;

import java.util.List;

/**
 * 团队业务接口。
 *
 * <p>群号不作为入参：它是请求的分区键，已由 {@code RobotAuthenticationFilter} 在认证阶段
 * 写入身份上下文，实现类统一用 {@code SecurityUtils.requireGroupNumber()} 取（DR-04）。
 * 签名里不出现它，「哪个方法忘了按群过滤」这类漏网之鱼也就无从产生。</p>
 */
public interface TeamService {
    TeamInfoVo createTeam(String msgContent);

    List<TeamInfoVo> getAvailableTeamList();

    List<TeamInfoVo> getHistoryTeamList();

    TeamDetailVo getTeamDetail(String msgContent);

    boolean removeTeam(String msgContent);
}
