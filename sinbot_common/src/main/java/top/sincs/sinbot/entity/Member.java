package top.sincs.sinbot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName(value = "member")
public class Member extends BaseEntity {

    @TableId(value = "member_id", type = IdType.ASSIGN_ID)
    private Long memberId;

    private Long teamId;

    private String memberQqNumber;

    private String memberName;

    private Long specialization;

    private Long subSpec;
}
