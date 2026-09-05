package com.study.dto;

import lombok.Data;
import javax.validation.constraints.NotNull;

/**
 * 用户登录DTO
 */
@Data
public class UserLoginDTO {

    @NotNull(message = "用户ID不能为空")
    private Long userId;
}
