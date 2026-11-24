package com.study.controller;

import com.study.common.Result;
import com.study.service.ScratchRewardService;
import com.study.vo.ScratchRewardVO;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;

@RestController
@RequestMapping("/rewards/scratch")
@RequiredArgsConstructor
public class ScratchRewardController {
    private final ScratchRewardService service;

    @GetMapping
    public Result<ScratchRewardVO> status(@RequestAttribute("userId") Long userId) {
        return Result.success(service.status(userId));
    }

    @PostMapping("/reveal")
    public Result<ScratchRewardVO> reveal(@RequestAttribute("userId") Long userId,
                                         @Valid @RequestBody RevealRequest request) {
        return Result.success(service.reveal(userId, request.getRewardDate()));
    }

    @Data
    public static class RevealRequest {
        @NotNull
        private LocalDate rewardDate;
    }
}
