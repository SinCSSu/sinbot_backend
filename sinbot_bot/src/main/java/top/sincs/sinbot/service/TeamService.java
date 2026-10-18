package top.sincs.sinbot.service;

import top.sincs.sinbot.vo.team.TeamDetailVo;
import top.sincs.sinbot.vo.team.TeamInfoVo;

import java.util.List;

public interface TeamService {
    TeamInfoVo CreateTeam(String groupNumber, String msgContent);

    List<TeamInfoVo> GetAvailableTeamList(String groupNumber);

    TeamDetailVo GetTeamInfo(String groupNumber, String msgContent);

    void removeTeam(String groupNumber, String msgContent);
}
