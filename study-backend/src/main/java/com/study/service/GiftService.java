package com.study.service;

import com.study.vo.GiftVO;
import java.util.List;

/**
 * 礼物服务接口
 */
public interface GiftService {

    /**
     * 获取所有可用礼物列表
     */
    List<GiftVO> getAvailableGifts();
}
