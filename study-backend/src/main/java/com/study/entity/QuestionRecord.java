package com.study.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 答题记录实体
 */
@Data
@TableName("question_records")
public class QuestionRecord implements Serializable {
    private static final long serialVersionUID = 1L;

    @TableId(value = "id", type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long questionId;

    private String subject;

    private String userAnswer;

    private Boolean isCorrect;

    private String correctAnswer;

    private String analysis;

    private Integer attemptCount;

    private Boolean firstAttemptCorrect;

    private Integer timeSpent;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime answeredAt;
}
