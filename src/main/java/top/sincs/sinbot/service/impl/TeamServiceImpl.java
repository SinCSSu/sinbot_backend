package top.sincs.sinbot.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import top.sincs.sinbot.constant.ErrorCode;
import top.sincs.sinbot.converter.MemberToMemberInfoVoConverter;
import top.sincs.sinbot.converter.TeamToTeamDetailVoConverter;
import top.sincs.sinbot.converter.TeamToTeamInfoVoConverter;
import top.sincs.sinbot.dto.team.CreateTeamDTO;
import top.sincs.sinbot.dto.team.UpdateSettlementDTO;
import top.sincs.sinbot.entity.Member;
import top.sincs.sinbot.entity.Team;
import top.sincs.sinbot.exception.BusinessException;
import top.sincs.sinbot.repository.MemberMapper;
import top.sincs.sinbot.repository.TeamMapper;
import top.sincs.sinbot.security.LoginUser;
import top.sincs.sinbot.security.SecurityUtils;
import top.sincs.sinbot.service.TeamService;
import top.sincs.sinbot.vo.member.MemberInfoVo;
import top.sincs.sinbot.vo.team.TeamDetailVo;
import top.sincs.sinbot.vo.team.TeamInfoVo;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TeamServiceImpl implements TeamService {

    /** 结算窗口长度：打完后仍允许回填黑本人 / 团队工资的时长（DR-17） */
    private static final long SETTLEMENT_WINDOW_HOURS = 3L;

    private final TeamMapper teamMapper;

    private final MemberMapper memberMapper;

    private final TeamToTeamInfoVoConverter teamToTeamInfoVoConverter;

    private final TeamToTeamDetailVoConverter teamToTeamDetailVoConverter;

    private final MemberToMemberInfoVoConverter memberToMemberInfoVoConverter;

    @Override
    public TeamInfoVo createTeam(String groupNumber, CreateTeamDTO team) {

        // DR-08：操作人不由调用方自称。机器人 X-Operator-Qq 与 App token 的 username
        // 值域一致（都是 QQ 号），在这里合流，业务侧无需区分来源。
        LoginUser operator = SecurityUtils.getLoginUserOrNull();
        String operatorId = operator == null ? null : operator.getUsername();

        Team teamInfo = new Team();
        teamInfo.setTeamName(team.getTeamName());
        teamInfo.setComment(team.getComment());
        teamInfo.setCreatedBy(operatorId);
        // DR-19：team_name 与 dungeon 各写各列，服务端不做复制或推导
        teamInfo.setDungeon(team.getDungeon());
        teamInfo.setGroupNumber(groupNumber);
        teamInfo.setStartTime(team.getStartTime());
        teamInfo.setLastUpdateBy(operatorId);
        teamInfo.setLimitMethod(team.getLimitMethod());
        teamInfo.setOvertimeTime(team.getStartTime().plusHours(SETTLEMENT_WINDOW_HOURS));

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
        return teamToTeamInfoVoConverter.ListConverter(selectRecruiting(groupNumber));
    }

    @Override
    public List<TeamInfoVo> getSettlementWindowTeamsByGroup(String groupNumber) {
        LocalDateTime now = LocalDateTime.now();
        return teamToTeamInfoVoConverter.ListConverter(teamMapper.selectList(
                Wrappers.<Team>lambdaQuery()
                        .eq(Team::getGroupNumber, groupNumber)
                        .le(Team::getStartTime, now)
                        .gt(Team::getOvertimeTime, now)
                        .orderByAsc(Team::getStartTime, Team::getTeamId)
        ));
    }

    @Override
    public List<TeamInfoVo> getTeamsByGroup(String groupNumber) {
        return teamToTeamInfoVoConverter.ListConverter(teamMapper.selectList(
                Wrappers.<Team>lambdaQuery()
                        .eq(Team::getGroupNumber, groupNumber)
                        .orderByAsc(Team::getStartTime, Team::getTeamId)
        ));
    }

    @Override
    public Team getRecruitingTeam(String groupNumber, Long teamId) {
        Team team = mustBeInGroup(groupNumber, teamId);
        // OQ-10：编号 → team_id 的换算由机器人完成，服务端不再重建列表取第 N 条。
        // 但阶段断言必须留下——列表不可见不等于不能报名，凭记忆拼 team_id 照样能打进来。
        assertRecruiting(team);
        return team;
    }

    @Override
    public TeamDetailVo getTeamDetail(String groupNumber, Long teamId) {
        Team team = mustBeInGroup(groupNumber, teamId);
        TeamDetailVo detail = teamToTeamDetailVoConverter.toDetail(team);
        detail.setMembers(listMembers(team.getTeamId()));
        return detail;
    }

    @Override
    public TeamInfoVo updateSettlement(String groupNumber, String operatorQq, Long teamId, UpdateSettlementDTO dto) {
        checkSettlementContent(dto);
        Team team = mustBeInGroup(groupNumber, teamId);

        LocalDateTime now = LocalDateTime.now();
        if (team.getStartTime().isAfter(now)) {
            throw new BusinessException(ErrorCode.TEAM_SETTLEMENT_NOT_STARTED);
        }
        if (!team.getOvertimeTime().isAfter(now)) {
            throw new BusinessException(ErrorCode.TEAM_SETTLEMENT_WINDOW_CLOSED);
        }
        return applySettlement(team, dto, operatorQq);
    }

    @Override
    public TeamInfoVo updateSettlementForAdmin(Long teamId, String operatorQq, UpdateSettlementDTO dto) {
        checkSettlementContent(dto);
        Team team = teamMapper.selectById(teamId);
        if (team == null) {
            throw new BusinessException(ErrorCode.TEAM_INFO_NOT_EXISTS);
        }
        // 管理端是漏结算的唯一补救入口，刻意不做任何时间窗口校验（DR-26）
        return applySettlement(team, dto, operatorQq);
    }

    private List<Team> selectRecruiting(String groupNumber) {
        return teamMapper.selectList(
                Wrappers.<Team>lambdaQuery()
                        .eq(Team::getGroupNumber, groupNumber)
                        .gt(Team::getStartTime, LocalDateTime.now())
                        // DR-15：team_id 兜底排序，保证同 start_time 时序号稳定
                        .orderByAsc(Team::getStartTime, Team::getTeamId)
        );
    }

    /**
     * 取「属于本群且未被逻辑删除」的团队。
     *
     * <p>服务端不再自行换算序号（OQ-10），于是这条群号校验成了唯一的越群防线：
     * team_id 由调用方传入，必须先证明它属于 {@code X-Group-Number} 指定的群。</p>
     */
    private Team mustBeInGroup(String groupNumber, Long teamId) {
        Team team = teamMapper.selectOne(Wrappers.<Team>lambdaQuery()
                .eq(Team::getTeamId, teamId)
                .eq(Team::getGroupNumber, groupNumber));
        if (team == null) {
            throw new BusinessException(ErrorCode.TEAM_NOT_IN_GROUP);
        }
        return team;
    }

    /** 「团队处于招募中」的唯一断言，报名与退团共用（DR-23） */
    private void assertRecruiting(Team team) {
        if (!team.getStartTime().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.TEAM_NOT_RECRUITING);
        }
    }

    private void checkSettlementContent(UpdateSettlementDTO dto) {
        if (dto == null || dto.isEmpty()) {
            throw new BusinessException(ErrorCode.SETTLEMENT_CONTENT_EMPTY);
        }
    }

    private TeamInfoVo applySettlement(Team team, UpdateSettlementDTO dto, String operatorQq) {
        teamMapper.update(null, Wrappers.<Team>lambdaUpdate()
                .eq(Team::getTeamId, team.getTeamId())
                .set(Team::getLocker, dto.getLocker())
                .set(Team::getSalary, dto.getSalary())
                .set(Team::getLastUpdateBy, operatorQq)
                .set(Team::getLastUpdateDate, LocalDateTime.now()));

        return teamToTeamInfoVoConverter.converter(teamMapper.selectById(team.getTeamId()));
    }

    private List<MemberInfoVo> listMembers(Long teamId) {
        List<Member> members = memberMapper.selectList(
                Wrappers.<Member>lambdaQuery()
                        .eq(Member::getTeamId, teamId)
                        .orderByAsc(Member::getMemberId)
        );
        return memberToMemberInfoVoConverter.listConverter(members);
    }
}
