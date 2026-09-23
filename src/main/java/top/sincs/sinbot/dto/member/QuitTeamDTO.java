package top.sincs.sinbot.dto.member;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 退团入参：指令「退团 团队编号 [id]」。
 *
 * <p>定位规则见 DR-24：按 {@code (team_id, member_qq_number)} 查未退团记录，
 * {@code memberName} 为空时要求命中唯一一条。</p>
 */
@Getter
@Setter
public class QuitTeamDTO {

    /** 目标团队主键，机器人换算后的结果（OQ-10） */
    @NotNull
    private Long teamId;

    /** 游戏 ID，报了多个角色时必填 */
    private String memberName;
}
