package top.sincs.sinbot.dto.team;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import top.sincs.sinbot.constant.DateConstant;

import java.time.LocalDateTime;

/**
 * 创建团队入参。
 *
 * <p>群号与操作人不在这个 DTO 里（DR-04 / DR-08）：群号取请求头 {@code X-Group-Number}，
 * 操作人从当前身份取——机器人走 {@code X-Operator-Qq}，App 走登录 token 里的 username。</p>
 */
@Getter
@Setter
public class CreateTeamDTO {

    /**
     * 副本名称，与 {@link #teamName} 分别由调用方录入（DR-19），服务端不做复制或推导。
     */
    @JsonProperty("dungeon")
    @NotBlank
    private String dungeon;

    @JsonProperty("team_name")
    @NotBlank
    private String teamName;

    private String comment;

    @NotNull
    @JsonProperty("start_time")
    @JsonFormat(pattern = DateConstant.DATE_TIME_FORMAT, timezone = DateConstant.TIME_ZONE)
    private LocalDateTime startTime;

    @JsonProperty("limit_method")
    private Long limitMethod;
}
