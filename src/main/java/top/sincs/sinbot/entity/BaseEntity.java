package top.sincs.sinbot.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public abstract class BaseEntity {

    @TableLogic(value = "N", delval = "Y")
    @TableField(value = "delete_flag", fill = FieldFill.INSERT)
    protected String deleteFlag;

    @TableField(value = "created_by", fill = FieldFill.INSERT)
    protected String createdBy;

    @TableField(value = "creation_date", fill = FieldFill.INSERT)
    protected LocalDateTime creationDate;

    @TableField(value = "last_update_by", fill = FieldFill.INSERT_UPDATE)
    protected String lastUpdateBy;

    @TableField(value = "last_update_date", fill = FieldFill.INSERT_UPDATE)
    protected LocalDateTime lastUpdateDate;
}
