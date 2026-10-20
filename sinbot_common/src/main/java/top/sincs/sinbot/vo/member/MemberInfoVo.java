package top.sincs.sinbot.vo.member;

import com.fasterxml.jackson.annotation.JsonProperty;
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

    @JsonProperty("member_id")
    private Long memberId;

    @JsonProperty("team_id")
    private Long teamId;

    @JsonProperty("member_qq_number")
    private String memberQqNumber;

    @JsonProperty("member_name")
    private String memberName;

    private Long specialization;

    @JsonProperty("sub_spec")
    private Long subSpec;
}
