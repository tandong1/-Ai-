package com.study.vo;

import lombok.Data;

/**
 * 用户信息VO
 */
@Data
public class UserVO {
    private Long id;
    private String name;
    private String avatar;
    private Integer totalPoints;
    private String currentLevel;
}
