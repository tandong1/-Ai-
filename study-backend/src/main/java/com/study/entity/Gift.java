package com.study.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 礼物实体
 */
@Data
@TableName(value = "gifts", autoResultMap = true)
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
    private Integer pointsRequired;

    /**
     * 礼物图片URL数组
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> images;

    /**
     * 库存数量（-1表示无限）
     */
    private Integer stock;

    /**
     * 是否上架
     */
    private Boolean isActive;

    /**
     * 排序权重
     */
    private Integer sortOrder;

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
