package top.sincs.sinbot.dto.team;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import top.sincs.sinbot.constant.DateConstant;

import java.time.LocalDateTime;

@Getter
@Setter
public class CreateTeamDTO {

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

    @JsonProperty("created_by")
    private String createdBy;

    @JsonProperty("group_number")
    @NotBlank
    private String groupNumber;
}
