package top.sincs.sinbot.dto.member;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 报名入参：指令「报名 团队编号 id 心法 [副心法]」。
 *
 * <p>取值分工见 DR-18：<br>
 * {@code teamId} → 机器人已完成「列表序号 → team_id」的换算（OQ-10），后端直接用；<br>
 * {@code memberName} → 指令里的「id」，即游戏 ID；<br>
 * 心法 → 机器人已翻译好的 {@code spec_id}，后端不校验（DR-27）；<br>
 * {@code memberQqNumber} 不在入参里，取 {@code X-Operator-Qq}。</p>
 */
@Getter
@Setter
public class SignUpDTO {

    @NotNull
    private Long teamId;

    @NotBlank
    private String memberName;

    @NotNull
    @JsonProperty(value = "member_spec")
    private Long specialization;

    /** 副心法可缺省 */
    private Long subSpec;
}
