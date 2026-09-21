package top.sincs.sinbot.entity;

import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public abstract class BaseEntity {

    @TableLogic(value = "N", delval = "Y")
    protected String deleteFlag;

    protected String createBy;

    protected LocalDateTime creationDate;

    protected String lastUpDateBy;

    protected LocalDateTime lastUpDateDate;
}
