package com.study.vo;

import lombok.Data;

@Data
public class ScratchRewardVO {
    private boolean eligible;
    private boolean revealed;
    private int points;
    private String message;
    private int totalPoints;
    private String rewardDate;
}
