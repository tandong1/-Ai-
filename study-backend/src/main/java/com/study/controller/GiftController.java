package com.study.controller;

import com.study.common.Result;
import com.study.service.GiftService;
import com.study.service.GiftExchangeService;
import com.study.dto.GiftExchangeDTO;
import jakarta.validation.Valid;
import java.util.Map;
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
    private final GiftExchangeService exchangeService;

    @PostMapping("/exchange")
    public Result<Map<String, Object>> exchange(@RequestAttribute("userId") Long userId,
            @Valid @RequestBody GiftExchangeDTO dto) {
        return Result.success(exchangeService.exchange(userId, dto));
    }

    @GetMapping("/balance")
    public Result<Map<String, Object>> balance(@RequestAttribute("userId") Long userId) {
        return Result.success(exchangeService.balance(userId));
    }

    @GetMapping("/exchanges")
    public Result<List<Map<String, Object>>> records(@RequestAttribute("userId") Long userId) {
        return Result.success(exchangeService.records(userId));
    }

    /**
     * 获取礼物列表
     */
    @GetMapping
    public Result<List<GiftVO>> getGifts() {
        List<GiftVO> gifts = giftService.getAvailableGifts();
        return Result.success(gifts);
    }
}
