package com.study.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.study.entity.Achievement;
import com.study.entity.User;
import com.study.mapper.AchievementMapper;
import com.study.mapper.QuestionRecordMapper;
import com.study.mapper.UserMapper;
import com.study.service.AchievementDefinition;
import com.study.service.AchievementService;
import com.study.vo.AchievementVO;
import com.study.vo.LearningStatsVO;
import com.study.vo.RecordSummaryVO;
import com.study.vo.UserAchievementsVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 成就服务实现
 *
 * 依赖的是 mapper 而不是 QuestionService，避免与 QuestionServiceImpl
 * （提交答题后需要评估成就）形成构造器注入的循环依赖
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AchievementServiceImpl implements AchievementService {

    private final AchievementMapper achievementMapper;
    private final QuestionRecordMapper questionRecordMapper;
    private final UserMapper userMapper;

    @Override
    public UserAchievementsVO getAchievements(Long userId) {
        log.info("获取成就墙: userId={}", userId);

        LearningStatsVO stats = getLearningStats(userId);
        Map<String, Achievement> unlockedMap = loadUnlockedMap(userId);

        // 先补发：老用户的历史记录可能早已满足条件，不能等到下次提交才解锁
        List<AchievementVO> newlyUnlocked = unlock(userId, stats, unlockedMap);
        if (!newlyUnlocked.isEmpty()) {
            // 重新读一次，拿到刚写入的 unlocked_at
            unlockedMap = loadUnlockedMap(userId);
        }

        List<AchievementVO> achievements = new ArrayList<>();
        int unlockedCount = 0;

        for (AchievementDefinition definition : AchievementDefinition.values()) {
            Achievement unlocked = unlockedMap.get(definition.getType());

            AchievementVO vo = toVO(definition);
            vo.setUnlocked(unlocked != null);
            vo.setUnlockedAt(unlocked == null ? null : unlocked.getUnlockedAt());

            if (unlocked != null) {
                unlockedCount++;
            }

            achievements.add(vo);
        }

        UserAchievementsVO result = new UserAchievementsVO();
        result.setUnlockedCount(unlockedCount);
        result.setTotalCount(achievements.size());
        result.setCurrentStreak(stats.getCurrentStreak());
        result.setMaxStreak(stats.getMaxStreak());
        result.setTotalQuestions(stats.getTotalQuestions());
        result.setAchievements(achievements);

        return result;
    }

    @Override
    public List<AchievementVO> evaluateAndUnlock(Long userId) {
        return unlock(userId, getLearningStats(userId), loadUnlockedMap(userId));
    }

    /**
     * 对照定义逐条解锁，返回本次新解锁的
     * 统计与已解锁记录由调用方传入，避免同一个请求里重复查库
     */
    private List<AchievementVO> unlock(Long userId, LearningStatsVO stats,
                                       Map<String, Achievement> unlockedMap) {
        List<AchievementVO> newlyUnlocked = new ArrayList<>();

        for (AchievementDefinition definition : AchievementDefinition.values()) {
            if (unlockedMap.containsKey(definition.getType())) {
                continue;
            }

            if (!definition.isSatisfiedBy(stats)) {
                continue;
            }

            // 依赖唯一索引做幂等，返回0说明并发下已被别的请求插入，不算本次新解锁
            int inserted = achievementMapper.insertIfAbsent(userId,
                    definition.getType(), definition.getName(), definition.getDesc(), definition.getIcon());

            if (inserted > 0) {
                log.info("解锁新成就: userId={}, type={}, name={}",
                        userId, definition.getType(), definition.getName());

                AchievementVO vo = toVO(definition);
                vo.setUnlocked(true);
                newlyUnlocked.add(vo);
            }
        }

        return newlyUnlocked;
    }

    @Override
    public LearningStatsVO getLearningStats(Long userId) {
        RecordSummaryVO summary = questionRecordMapper.selectRecordSummary(userId, null);
        List<LocalDate> dates = questionRecordMapper.selectDistinctAnswerDates(userId);

        LearningStatsVO stats = new LearningStatsVO();
        stats.setTotalDays(summary == null || summary.getTotalDays() == null ? 0 : summary.getTotalDays());
        stats.setTotalQuestions(summary == null || summary.getTotalQuestions() == null ? 0 : summary.getTotalQuestions());
        stats.setHasPerfect(summary != null && Boolean.TRUE.equals(summary.getHasPerfect()));
        stats.setMaxStreak(calculateMaxStreak(dates));
        stats.setCurrentStreak(calculateCurrentStreak(dates));

        User user = userMapper.selectById(userId);
        stats.setTotalPoints(user == null || user.getTotalPoints() == null ? 0 : user.getTotalPoints());

        return stats;
    }

    /**
     * 最长连续答题天数。用于成就判定：一旦达成就不应因后来断签而回收
     */
    private int calculateMaxStreak(List<LocalDate> sortedDates) {
        if (sortedDates == null || sortedDates.isEmpty()) {
            return 0;
        }

        int maxStreak = 1;
        int streak = 1;

        for (int i = 1; i < sortedDates.size(); i++) {
            long gap = ChronoUnit.DAYS.between(sortedDates.get(i - 1), sortedDates.get(i));

            if (gap == 1) {
                streak++;
                maxStreak = Math.max(maxStreak, streak);
            } else if (gap > 1) {
                streak = 1;
            }
            // gap == 0 理论上不会出现（日期已去重），出现也不应打断连续
        }

        return maxStreak;
    }

    /**
     * 当前连续答题天数。以今天或昨天结尾才算还在延续中，
     * 昨天结尾也算是为了不让"今天还没做"就把进度显示成0
     */
    private int calculateCurrentStreak(List<LocalDate> sortedDates) {
        if (sortedDates == null || sortedDates.isEmpty()) {
            return 0;
        }

        LocalDate today = LocalDate.now();
        LocalDate last = sortedDates.get(sortedDates.size() - 1);
        long sinceLast = ChronoUnit.DAYS.between(last, today);

        if (sinceLast > 1) {
            return 0;
        }

        int streak = 1;
        for (int i = sortedDates.size() - 1; i > 0; i--) {
            if (ChronoUnit.DAYS.between(sortedDates.get(i - 1), sortedDates.get(i)) == 1) {
                streak++;
            } else {
                break;
            }
        }

        return streak;
    }

    private Map<String, Achievement> loadUnlockedMap(Long userId) {
        LambdaQueryWrapper<Achievement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Achievement::getUserId, userId);

        Map<String, Achievement> map = new HashMap<>();
        achievementMapper.selectList(wrapper)
                .forEach(achievement -> map.put(achievement.getAchievementType(), achievement));

        return map;
    }

    private AchievementVO toVO(AchievementDefinition definition) {
        AchievementVO vo = new AchievementVO();
        vo.setType(definition.getType());
        vo.setName(definition.getName());
        vo.setDesc(definition.getDesc());
        vo.setIcon(definition.getIcon());
        return vo;
    }
}
