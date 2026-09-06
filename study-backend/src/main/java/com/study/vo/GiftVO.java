package com.study.vo;

import lombok.Data;
import java.util.List;

/**
 * 礼物VO
 */
@Data
public class GiftVO {
    private Long id;
    private String name;
    private String description;
    private Integer points;
    private List<String> images;
    private Integer stock;
}
