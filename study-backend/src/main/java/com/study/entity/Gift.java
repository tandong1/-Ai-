package com.study.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 礼物实体
 */
@Data
@TableName("gifts")
public class Gift implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 礼物名称
     */
    private String name;

    /**
     * 礼物描述
     */
    private String description;

    /**
     * 所需积分
     */
    private Integer points;

    /**
     * 礼物图片URL
     */
    private String imageUrl;

    /**
     * 库存数量
     */
    private Integer stock;

    /**
     * 状态: active/inactive
     */
    private String status;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updatedAt;
}
