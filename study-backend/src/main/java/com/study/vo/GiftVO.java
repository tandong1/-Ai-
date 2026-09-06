package com.study.vo;

import lombok.Data;

/**
 * 礼物VO
 */
@Data
public class GiftVO {
    private Long id;
    private String name;
    private String description;
    private Integer points;
    private String imageUrl;
    private Integer stock;
    private String status;
}
