package top.sincs.sinbot.vo.team;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class TeamInfoVo {
    private Long teamId;

    private String teamName;

    private String groupNumber;

    private String dungeon;

    private String comment;

    private LocalDateTime startTime;

    private LocalDateTime overtimeTime;

    private Long limitMethod;

    private String locker;

    private Integer salary;

    private String createdBy;
}
