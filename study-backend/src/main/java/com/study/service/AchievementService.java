package com.study.service;

import com.study.vo.AchievementVO;
import com.study.vo.LearningStatsVO;
import com.study.vo.UserAchievementsVO;

import java.util.List;

/**
 * 成就服务接口
 */
public interface AchievementService {

    /**
     * 获取成就墙（先评估并补发新解锁的，再返回全部成就）
     */
    UserAchievementsVO getAchievements(Long userId);

    /**
     * 评估并解锁成就
     *
     * @return 本次新解锁的成就，没有则为空列表
     */
    List<AchievementVO> evaluateAndUnlock(Long userId);

    /**
     * 计算学习统计（成就判定的输入）
     */
    LearningStatsVO getLearningStats(Long userId);
}
