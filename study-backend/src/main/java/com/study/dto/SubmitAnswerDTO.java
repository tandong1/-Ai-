package com.study.dto;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

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
        private Boolean isCorrect;
        private Integer attemptCount;
    }
}
