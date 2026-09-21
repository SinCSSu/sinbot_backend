package top.sincs.sinbot.dto.team;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class CreateTeamDTO {

    @JsonProperty("team_name")
    private String teamName;

    private String comment;

    @JsonProperty("start_time")
    private LocalDateTime startTime;

    @JsonProperty("limit_method")
    private String limitMethod;

    @JsonProperty("created_by")
    private String createdBy;

    @JsonProperty("group_number")
    private String groupNumber;
}
