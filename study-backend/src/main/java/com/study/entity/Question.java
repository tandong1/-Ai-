package com.study.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 题目实体
 */
@Data
@TableName(value = "questions", autoResultMap = true)
public class Question implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    /**
     * 目标用户ID
     */
    private Long userId;

    /**
     * 科目: math/english/chinese
     */
    private String subject;

    /**
     * 难度: easy/medium/hard
     */
    private String difficulty;

    /**
     * 题型: choice/fill
     */
    private String questionType;

    /**
     * 题目内容
     */
    private String questionText;

    /**
     * 选项（选择题）
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> options;

    /**
     * 正确答案
     */
    private String correctAnswer;

    /**
     * 题目解析
     */
    private String analysis;

    /**
     * 关联知识点
     */
    private String knowledgePoint;

    /**
     * 来源知识库ID
     */
    private Long knowledgeBaseId;

    /**
     * 生成日期
     */
    private LocalDate generatedDate;

    /**
     * 是否已被作答
     */
    private Boolean isUsed;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
}
