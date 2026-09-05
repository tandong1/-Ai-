package com.study.vo;

import lombok.Data;
import java.util.List;

/**
 * 提交答案结果VO
 */
@Data
public class SubmitResultVO {
    private List<AnswerResultVO> results;
    private Integer totalPointsEarned;
    private Integer correctCount;
    private Integer totalCount;
    private Double accuracy;
    private Integer newBalance;
}
