package top.sincs.sinbot.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import top.sincs.sinbot.constant.ErrorCode;
import top.sincs.sinbot.converter.MemberToMemberInfoVoConverter;
import top.sincs.sinbot.dto.member.QuitTeamDTO;
import top.sincs.sinbot.dto.member.SignUpDTO;
import top.sincs.sinbot.entity.Member;
import top.sincs.sinbot.entity.Team;
import top.sincs.sinbot.exception.BusinessException;
import top.sincs.sinbot.repository.MemberMapper;
import top.sincs.sinbot.service.MemberService;
import top.sincs.sinbot.service.TeamService;
import top.sincs.sinbot.vo.member.MemberInfoVo;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MemberServiceImpl implements MemberService {

    private final MemberMapper memberMapper;

    private final TeamService teamService;

    private final MemberToMemberInfoVoConverter memberToMemberInfoVoConverter;

    @Override
    public MemberInfoVo signUp(String groupNumber, String operatorQq, SignUpDTO dto) {
        // 机器人已把列表序号换算成 team_id（OQ-10）；这里只做「属于本群」兜底 + 阶段断言，
        // 报名与退团共用同一个断言（DR-16 / DR-23）
        Team team = teamService.getRecruitingTeam(groupNumber, dto.getTeamId());

        if (alreadySignedUp(team.getTeamId(), dto.getMemberName())) {
            throw new BusinessException(ErrorCode.DATA_ALREADY_EXISTS, "已用该游戏ID报名过本团");
        }

        Member member = new Member();
        member.setTeamId(team.getTeamId());
        // QQ 号不来自指令参数，来自请求头（DR-08 / DR-18）
        member.setMemberQqNumber(operatorQq);
        member.setMemberName(dto.getMemberName());
        member.setSpecialization(dto.getSpecialization());
        member.setSubSpec(dto.getSubSpec());
        member.setCreatedBy(operatorQq);
        member.setLastUpdateBy(operatorQq);

        memberMapper.insert(member);

        return memberToMemberInfoVoConverter.converter(member);
    }

    @Override
    public void quitTeam(String groupNumber, String operatorQq, QuitTeamDTO dto) {
        Team team = teamService.getRecruitingTeam(groupNumber, dto.getTeamId());

        List<Member> signedUp = memberMapper.selectList(
                Wrappers.<Member>lambdaQuery()
                        .eq(Member::getTeamId, team.getTeamId())
                        .eq(Member::getMemberQqNumber, operatorQq)
                        .eq(StringUtils.hasText(dto.getMemberName()), Member::getMemberName, dto.getMemberName())
                        .orderByAsc(Member::getMemberId)
        );

        if (signedUp.isEmpty()) {
            throw new BusinessException(ErrorCode.MEMBER_NOT_SIGNED_UP);
        }
        if (signedUp.size() > 1) {
            // 给了 memberName 时不可能走到这里：(team_id, member_name) 唯一（DR-21）
            throw new BusinessException(ErrorCode.MEMBER_RECORD_AMBIGUOUS);
        }

        // DR-22：逻辑删除，同一个游戏 ID 之后还能重新报名
        signedUp.forEach(member -> memberMapper.deleteById(member.getMemberId()));
    }

    /**
     * DR-21：唯一性只在应用层校验，禁止建 (team_id, member_name) 唯一索引——
     * 已退团的记录仍在表里，建了索引会让重新报名被数据库拒绝。
     *
     * <p>这里不加 QQ 号条件：同一个团里同名即重复，与谁报的无关。</p>
     */
    private boolean alreadySignedUp(Long teamId, String memberName) {
        Long count = memberMapper.selectCount(
                Wrappers.<Member>lambdaQuery()
                        .eq(Member::getTeamId, teamId)
                        .eq(Member::getMemberName, memberName)
        );
        return count != null && count > 0;
    }
}
