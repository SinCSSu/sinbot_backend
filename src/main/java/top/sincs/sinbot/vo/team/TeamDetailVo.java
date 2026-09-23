package top.sincs.sinbot.vo.team;

import lombok.Getter;
import lombok.Setter;
import top.sincs.sinbot.vo.member.MemberInfoVo;

import java.util.List;

/**
 * 团队详情：团队信息 + 当前报名名单（DR-16「查看团队 团队编号」）。
 */
@Getter
@Setter
public class TeamDetailVo extends TeamInfoVo {

    /** 未退团的报名记录，按报名先后升序 */
    private List<MemberInfoVo> members;
}
