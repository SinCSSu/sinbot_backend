package top.sincs.sinbot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@TableName("team")
@AllArgsConstructor
@NoArgsConstructor
public class Team extends BaseEntity {
    @TableId(value = "team_id" ,type = IdType.ASSIGN_ID)
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

}
