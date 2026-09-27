package com.study.controller;

import com.study.common.Result;
import com.study.dto.UserLoginDTO;
import com.study.service.AchievementService;
import com.study.service.UserService;
import com.study.vo.LoginVO;
import com.study.vo.UserAchievementsVO;
import com.study.vo.UserVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户控制器
 */
@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final AchievementService achievementService;

    /**
     * 获取用户列表
     */
    @GetMapping
    public Result<List<UserVO>> listUsers() {
        List<UserVO> users = userService.listUsers();
        return Result.success(users);
    }

    /**
     * 获取成就墙（含未解锁项）
     * 读取时会顺带补发已满足条件但尚未入库的成就，历史用户首次打开即可回填
     */
    @GetMapping("/achievements")
    public Result<UserAchievementsVO> getAchievements(
            @RequestAttribute("userId") Long userId) {
        UserAchievementsVO achievements = achievementService.getAchievements(userId);
        return Result.success(achievements);
    }

    /**
     * 用户登录
     */
    @PostMapping("/login")
    public Result<LoginVO> login(@RequestBody @Validated UserLoginDTO loginDTO) {
        LoginVO loginVO = userService.login(loginDTO);
        return Result.success(loginVO);
    }
}
