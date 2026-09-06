package com.study.dto;

import lombok.Data;
import jakarta.validation.constraints.NotNull;

/**
 * 用户登录DTO
 */
@Data
public class UserLoginDTO {

    @NotNull(message = "用户ID不能为空")
    private Long userId;
}
