package top.sincs.sinbot.vo.member;

import lombok.Getter;
import lombok.Setter;

/**
 * 报名记录视图。
 *
 * <p>展示以 {@code memberName}（游戏 ID）为主标识，QQ 号仅作标识符（DR-10）。</p>
 */
@Getter
@Setter
public class MemberInfoVo {

    private Long memberId;

    private Long teamId;

    private String memberQqNumber;

    private String memberName;

    private Long specialization;

    private Long subSpec;
}
