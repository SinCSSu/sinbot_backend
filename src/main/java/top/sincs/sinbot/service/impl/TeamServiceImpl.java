package top.sincs.sinbot.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import io.netty.util.internal.ObjectUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import top.sincs.sinbot.constant.ErrorCode;
import top.sincs.sinbot.converter.TeamToTeamInfoVoConverter;
import top.sincs.sinbot.dto.team.CreateTeamDTO;
import top.sincs.sinbot.entity.Team;
import top.sincs.sinbot.exception.BusinessException;
import top.sincs.sinbot.repository.TeamMapper;
import top.sincs.sinbot.service.TeamService;
import top.sincs.sinbot.vo.team.TeamInfoVo;

import java.sql.Wrapper;
import java.util.Date;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamServiceImpl implements TeamService {

    private final TeamMapper teamMapper;

    private final TeamToTeamInfoVoConverter teamToTeamInfoVoConverter;

    @Override
    public TeamInfoVo createTeam(CreateTeamDTO team) {

        Team teamInfo = new Team();
        teamInfo.setTeamName(team.getTeamName());
        teamInfo.setComment(team.getComment());
        teamInfo.setCreatedBy(team.getCreatedBy());
        teamInfo.setDungeon(team.getComment());
        teamInfo.setGroupNumber(team.getGroupNumber());
        teamInfo.setStartTime(team.getStartTime());
        teamInfo.setLastUpdateBy(team.getCreatedBy());
        teamInfo.setLimitMethod(team.getLimitMethod());
        teamInfo.setOvertimeTime(team.getStartTime().plusHours(3L));

        teamMapper.insert(teamInfo);

        Team teamResult = teamMapper.selectById(teamInfo.getTeamId());

        return teamToTeamInfoVoConverter.converter(teamResult);
    }

    @Override
    public void deleteTeam(Long teamId) {
        Team teamInfo = teamMapper.selectById(teamId);
        if (teamInfo == null) {
            throw new BusinessException(ErrorCode.TEAM_INFO_NOT_EXISTS);
        }
        teamMapper.deleteById(teamId);
    }

    @Override
    public List<TeamInfoVo> getAvailableTeamByGroup(String groupNumber) {
        return teamToTeamInfoVoConverter.ListConverter(teamMapper.selectList(
                Wrappers.<Team>lambdaQuery()
                        .eq(Team::getGroupNumber, groupNumber)
                        .ge(Team::getStartTime, new Date())
                        .orderByAsc(Team::getTeamId)
        ));
    }
}


