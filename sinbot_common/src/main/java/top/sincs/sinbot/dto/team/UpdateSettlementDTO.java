package top.sincs.sinbot.dto.team;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * 结算信息入参：黑本人（游戏 ID）与人均工资（金）。
 *
 * <p>语义见 DR-26：两列都是回填字段，写错可直接覆盖写入，不留版本。
 * 两者至少传一个，避免「只传一个把另一个清空」。</p>
 */
@Getter
@Setter
public class UpdateSettlementDTO {

    /** 黑本人的游戏 ID，单值（DR-26） */
    private String locker;

    /** 人均工资，单位金，只取整数（DR-26） */
    private Integer salary;

    public boolean isEmpty() {
        return locker == null && salary == null;
    }
}
