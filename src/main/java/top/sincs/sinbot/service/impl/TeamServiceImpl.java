package top.sincs.sinbot.service.impl;

import org.springframework.stereotype.Service;
import top.sincs.sinbot.dto.team.CreateTeamDTO;
import top.sincs.sinbot.service.TeamService;
import top.sincs.sinbot.vo.team.TeamInfoVo;

@Service
public class TeamServiceImpl implements TeamService {
    @Override
    public TeamInfoVo createTeam(CreateTeamDTO team) {
        return new TeamInfoVo();
    }
}
