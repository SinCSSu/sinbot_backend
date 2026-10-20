package top.sincs.sinbot.service.impl;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import top.sincs.sinbot.constant.ErrorCode;
import top.sincs.sinbot.converter.MemberToMemberInfoVoConverter;
import top.sincs.sinbot.converter.TeamToTeamDetailVoConverter;
import top.sincs.sinbot.converter.TeamToTeamInfoVoConverter;
import top.sincs.sinbot.entity.Member;
import top.sincs.sinbot.entity.Team;
import top.sincs.sinbot.exception.BusinessException;
import top.sincs.sinbot.repository.MemberMapper;
import top.sincs.sinbot.repository.TeamMapper;
import top.sincs.sinbot.security.SecurityUtils;
import top.sincs.sinbot.service.TeamService;
import top.sincs.sinbot.utils.DateUtils;
import top.sincs.sinbot.vo.team.TeamDetailVo;
import top.sincs.sinbot.vo.team.TeamInfoVo;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TeamServiceImpl implements TeamService {

    private final TeamMapper teamMapper;

    private final MemberMapper memberMapper;

    private final TeamToTeamInfoVoConverter teamToTeamInfoVoConverter;

    private final TeamToTeamDetailVoConverter teamToTeamDetailVoConverter;

    private final MemberToMemberInfoVoConverter memberToMemberInfoVoConverter;

    //今晚八点 yx
    //yx 今晚八点
    //yx 今晚八点 有事别报
    //yx 今晚八点 无限制 有事别报
    @Override
    public TeamInfoVo createTeam(String msgContent) {
        long OVERTIME_LENGTH = 3L;

        // DR-04：群号取自身份上下文，不由调用方传入
        String groupNumber = SecurityUtils.requireGroupNumber();
        log.info("创建团队，来自群{} 操作人{}", groupNumber, SecurityUtils.getLoginUser().getUsername());

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
        // 落库后必须仍是「招募中」：过去的时间会让团队直接进入结算窗口甚至归档，
        // 群里两个列表都查不到、也报不了名，还会污染「查询历史团队」（DR-13 / DR-16）
        if (!startTime.isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.CREATE_TEAM_PARAM_ERROR, "开团时间必须晚于当前时间");
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
        // DR-19（2026-10-20 修订）：dungeon 一期无独立业务语义，复用团名填充
        newTeam.setDungeon(newTeam.getTeamName());
        newTeam.setGroupNumber(groupNumber);

        teamMapper.insert(newTeam);

        return teamToTeamInfoVoConverter.converter(newTeam);
    }

    @Override
    public List<TeamInfoVo> getAvailableTeamList() {
        String groupNumber = SecurityUtils.requireGroupNumber();
        List<Team> teamList = teamMapper.selectList(
                new LambdaQueryWrapper<Team>()
                        .eq(Team::getGroupNumber, groupNumber)
                        .gt(Team::getStartTime, LocalDateTime.now())
                        .orderByAsc(Team::getTeamId));

        return teamToTeamInfoVoConverter.ListConverter(teamList);
    }

    @Override
    public List<TeamInfoVo> getHistoryTeamList() {
        String groupNumber = SecurityUtils.requireGroupNumber();
        List<Team> teamList = teamMapper.selectList(
                new LambdaQueryWrapper<Team>()
                        .eq(Team::getGroupNumber, groupNumber)
                        .gt(Team::getOvertimeTime, LocalDateTime.now())
                        .le(Team::getStartTime, LocalDateTime.now())
                        .orderByAsc(Team::getTeamId));
        return teamToTeamInfoVoConverter.ListConverter(teamList);
    }

    @Override
    public TeamDetailVo getTeamDetail(String msgContent) {
        Team targetTeam = parseAvailableTeamFromMsg(msgContent);
        TeamDetailVo result = teamToTeamDetailVoConverter.toDetail(targetTeam);
        List<Member> members = memberMapper.selectList(
                new LambdaQueryWrapper<Member>()
                        .eq(Member::getTeamId, targetTeam.getTeamId())
        );
        result.setMembers(
                memberToMemberInfoVoConverter.listConverter(members)
        );
        return result;
    }

    /**
     * 解散团队。
     *
     * <p>返回值只有「成功」这一种语义：只要方法正常返回就是 {@code true}，
     * 其余任何失败（解析不到团队、已被并发删除等）一律以异常返回。</p>
     *
     * <p>报名记录一并逻辑删除，否则会留下指向已解散团队的孤儿记录。</p>
     */
    @Override
    @Transactional
    public boolean removeTeam(String msgContent) {
        Team targetTeam = parseAvailableTeamFromMsg(msgContent);
        memberMapper.delete(new LambdaQueryWrapper<Member>()
                .eq(Member::getTeamId, targetTeam.getTeamId()));
        if (teamMapper.deleteById(targetTeam.getTeamId()) == 0) {
            throw new BusinessException(ErrorCode.TEAM_INFO_NOT_EXISTS);
        }
        return true;
    }

    /** 按群号（取自身份上下文）+ 当前在开的团解析出目标团队 */
    private Team parseAvailableTeamFromMsg(String msgContent) {
        String groupNumber = SecurityUtils.requireGroupNumber();
        List<Team> teamList = teamMapper.selectList(
                new LambdaQueryWrapper<Team>()
                        .eq(Team::getGroupNumber, groupNumber)
                        .gt(Team::getStartTime, LocalDateTime.now())
                        .orderByAsc(Team::getTeamId));
        if (teamList == null || teamList.isEmpty()) {
            throw new BusinessException(ErrorCode.NO_AVAILABLE_TEAM);
        }

        if (StrUtil.isEmpty(msgContent)) {
            if (teamList.size() == 1) {
                return teamList.getFirst();
            } else {
                throw new BusinessException(ErrorCode.MULTI_AVAILABLE_TEAM);
            }
        }
        if (StrUtil.isNumeric(msgContent)) {
            int teamNo = Integer.parseInt(msgContent);
            if (teamNo < 1 || teamNo > teamList.size()) {
                throw new BusinessException(ErrorCode.TEAM_NUMBER_OUT_OF_RANGE, "团队序号不存在");
            } else {
                return teamList.get(teamNo - 1);
            }
        } else {
            List<Team> targetTeam = teamList.stream().filter((team) -> {
                return team.getTeamName().equals(msgContent);
            }).toList();
            if (targetTeam.isEmpty()) {
                throw new BusinessException(ErrorCode.NO_AVAILABLE_TEAM, "无法找到所输入的团队");
            } else if (targetTeam.size() > 1) {
                throw new BusinessException(ErrorCode.MULTI_AVAILABLE_TEAM,
                        StrUtil.format("存在多个名为 {} 的团队，请使用序号指定", msgContent));
            } else {
                return targetTeam.getFirst();
            }
        }

    }
}
