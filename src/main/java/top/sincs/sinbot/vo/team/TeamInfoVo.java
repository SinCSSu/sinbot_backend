package top.sincs.sinbot.vo.team;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;
import top.sincs.sinbot.constant.DateConstant;

import java.time.LocalDateTime;

@Getter
@Setter
public class TeamInfoVo {
    @JsonProperty(value = "team_id")
    private String teamId;

    @JsonProperty(value = "team_name")
    private String teamName;

    @JsonProperty(value = "group_number")
    private String groupNumber;

    private String dungeon;

    private String comment;

    @JsonProperty(value = "start_time")
    @JsonFormat(pattern = DateConstant.DATE_TIME_FORMAT, timezone = DateConstant.TIME_ZONE)
    private LocalDateTime startTime;

    @JsonProperty(value = "overtime_time")
    @JsonFormat(pattern = DateConstant.DATE_TIME_FORMAT, timezone = DateConstant.TIME_ZONE)
    private LocalDateTime overtimeTime;

    @JsonProperty(value = "limit_method")
    private String limitMethod;

    private String locker;

    private Integer salary;

    @JsonProperty(value = "create_by")
    private String createdBy;
}
