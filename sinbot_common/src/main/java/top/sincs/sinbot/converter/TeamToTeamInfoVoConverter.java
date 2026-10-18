package top.sincs.sinbot.converter;

import org.mapstruct.Mapper;
import top.sincs.sinbot.entity.Team;
import top.sincs.sinbot.vo.team.TeamInfoVo;

import java.util.List;

@Mapper(componentModel = "spring")
public interface TeamToTeamInfoVoConverter {
    TeamInfoVo converter(Team team);

    List<TeamInfoVo> ListConverter(List<Team> teamList);
}
