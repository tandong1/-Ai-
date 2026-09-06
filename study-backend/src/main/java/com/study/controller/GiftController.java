package com.study.controller;

import com.study.common.Result;
import com.study.service.GiftService;
import com.study.vo.GiftVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 礼物控制器
 */
@Slf4j
@RestController
@RequestMapping("/gifts")
@RequiredArgsConstructor
public class GiftController {

    private final GiftService giftService;

    /**
     * 获取礼物列表
     */
    @GetMapping
    public Result<List<GiftVO>> getGifts() {
        List<GiftVO> gifts = giftService.getAvailableGifts();
        return Result.success(gifts);
    }
}
