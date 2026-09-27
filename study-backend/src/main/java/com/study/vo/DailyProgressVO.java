package com.study.vo;

import lombok.Data;

/**
 * 今日各科目完成状态VO
 */
@Data
public class DailyProgressVO {

    private Boolean math;

    private Boolean english;

    private Boolean chinese;

    public DailyProgressVO() {
        this.math = false;
        this.english = false;
        this.chinese = false;
    }
}
