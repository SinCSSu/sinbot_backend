package top.sincs.sinbot.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@TableName(value = "bd_specialization")
public class Specialization extends BaseEntity {
    @TableId(value = "spec_id", type = IdType.ASSIGN_ID)
    private Long specId;

    private Long relateSpecId;

    private String specName;

    private String specNickname;

    private Integer color;

    private String iconPath;

    private Integer specType;
}
