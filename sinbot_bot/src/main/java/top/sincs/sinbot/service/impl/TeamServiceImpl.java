package top.sincs.sinbot.service.impl;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import top.sincs.sinbot.constant.ErrorCode;
import top.sincs.sinbot.converter.TeamToTeamInfoVoConverter;
import top.sincs.sinbot.entity.Team;
import top.sincs.sinbot.exception.BusinessException;
import top.sincs.sinbot.repository.TeamMapper;
import top.sincs.sinbot.service.TeamService;
import top.sincs.sinbot.utils.DateUtils;
import top.sincs.sinbot.vo.team.TeamDetailVo;
import top.sincs.sinbot.vo.team.TeamInfoVo;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamServiceImpl implements TeamService {

    private final TeamMapper teamMapper;

    private final TeamToTeamInfoVoConverter teamToTeamInfoVoConverter;

    //开团 今晚八点 yx
    //开团 yx 今晚八点
    //开团 yx 今晚八点 有事别报
    //开团 yx 今晚八点 无限制 有事别报
    @Override
    public TeamInfoVo CreateTeam(String groupNumber, String msgContent) {
        long OVERTIME_LENGTH = 3L;


        List<String> commandList = new java.util.ArrayList<>(List.of(msgContent.split(" ")));
        if (commandList.size() > 4 || commandList.size() < 2) {
            throw new BusinessException(ErrorCode.CREATE_TEAM_PARAM_ERROR, "请输入正确的参数");
        }
        LocalDateTime startTime = null;
        Team newTeam = new Team();
        try {
            startTime = DateUtils.parseChineseDateToLocalDateTime(commandList.getFirst());
        } catch (Exception ignored) {
        }
        if (startTime == null) {
            try {
                startTime = DateUtils.parseChineseDateToLocalDateTime(commandList.get(1));
            } catch (Exception ignored) {
            }
            newTeam.setTeamName(commandList.getFirst());
        } else {
            newTeam.setTeamName(commandList.get(1));
        }
        if (startTime == null) {
            throw new BusinessException(ErrorCode.CREATE_TEAM_PARAM_ERROR, "请输入正确的开团时间");
        }
        newTeam.setStartTime(startTime);
        newTeam.setOvertimeTime(startTime.plusHours(OVERTIME_LENGTH));

        if (commandList.contains("无限制")) {
            newTeam.setLimitMethod(0L);
            commandList.remove("无限制");
        } else {
            newTeam.setLimitMethod(1L);
        }
        if (commandList.size() > 2) {
            newTeam.setComment(String.join(" ", commandList.subList(2, commandList.size())));
        }
        newTeam.setDungeon(newTeam.getTeamName());
        newTeam.setGroupNumber(groupNumber);

        teamMapper.insert(newTeam);

        return teamToTeamInfoVoConverter.converter(newTeam);
    }

    @Override
    public List<TeamInfoVo> GetAvailableTeamList(String groupNumber) {
        return List.of();
    }

    @Override
    public TeamDetailVo GetTeamInfo(String groupNumber, String msgContent) {
        return null;
    }

    @Override
    public void removeTeam(String groupNumber, String msgContent) {

    }
}
