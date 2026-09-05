package com.study.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 错题本实体
 */
@Data
@TableName("wrong_questions")
public class WrongQuestion implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 题目ID
     */
    private Long questionId;

    /**
     * 科目
     */
    private String subject;

    /**
     * 首次答错时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime firstWrongAt;

    /**
     * 答错次数
     */
    private Integer wrongCount;

    /**
     * 是否已掌握
     */
    private Boolean isMastered;

    /**
     * 掌握时间
     */
    private LocalDateTime masteredAt;

    /**
     * 最后复习时间
     */
    private LocalDateTime lastReviewAt;
}
