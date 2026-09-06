package com.study.dto;

  import lombok.Data;
  import jakarta.validation.constraints.NotBlank;
  import jakarta.validation.constraints.NotEmpty;
  import java.util.List;

  /**
   * 提交答案DTO
   */
  @Data
  public class SubmitAnswerDTO {
  
      @NotBlank(message = "科目不能为空")
      private String subject;

      @NotEmpty(message = "答案列表不能为空")
      private List<AnswerItem> answers;
                                         
      @Data
      public static class AnswerItem {
          private Long questionId;
          private String userAnswer;
          private Boolean isCorrect;      // 前端已判断是否正确
          private Integer attemptCount;   // 尝试次数（1或2）
      }
  }

  2️⃣ SubmitResultVO.java (完整替换)
                                         
  package com.study.vo;

  import lombok.Data;

  /**
   * 提交答案结果VO                      
   */
  @Data
  public class SubmitResultVO {
      private String subject;
      private Integer totalQuestions;    
      private Integer correctCount;
      private Integer pointsEarned;    // 固定为1（完成一个科目）
      private Integer newBalance;
  }
