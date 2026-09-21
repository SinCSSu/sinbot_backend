package top.sincs.sinbot.service;

import org.springframework.stereotype.Service;
import top.sincs.sinbot.dto.team.CreateTeamDTO;
import top.sincs.sinbot.vo.team.TeamInfoVo;

@Service
public interface TeamService {
    TeamInfoVo createTeam(CreateTeamDTO team);
}
