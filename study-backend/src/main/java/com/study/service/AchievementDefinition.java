package com.study.service;

import com.study.vo.LearningStatsVO;

import java.util.function.Predicate;

/**
 * 成就定义，唯一事实源
 *
 * 枚举顺序即成就墙的展示顺序。
 * type 一旦上线不要再改：它是 achievements 表的业务主键，阈值编在 type 里
 * （如 streak_10），将来调整阈值应当新增一个 type，而不是改旧 type 的含义。
 * name/desc/icon 可以随时改文案，接口返回的始终是这里的当前值。
 */
public enum AchievementDefinition {

    FIRST_WIN("first_win", "首战告捷", "完成第一次挑战", "🎯",
            stats -> stats.getTotalDays() >= 1),

    STREAK_10("streak_10", "坚持不懈", "连续学习10天", "🔥",
            stats -> stats.getMaxStreak() >= 10),

    STREAK_30("streak_30", "学习达人", "连续学习30天", "💪",
            stats -> stats.getMaxStreak() >= 30),

    QUESTIONS_1000("questions_1000", "千题斩", "累计答题1000题", "📝",
            stats -> stats.getTotalQuestions() >= 1000),

    PERFECT("perfect", "全对高手", "单次全对", "✨",
            stats -> Boolean.TRUE.equals(stats.getHasPerfect())),

    POINTS_500("points_500", "积分大户", "累计500积分", "💰",
            stats -> stats.getTotalPoints() >= 500);

    private final String type;
    private final String name;
    private final String desc;
    private final String icon;
    private final Predicate<LearningStatsVO> condition;

    AchievementDefinition(String type, String name, String desc, String icon,
                          Predicate<LearningStatsVO> condition) {
        this.type = type;
        this.name = name;
        this.desc = desc;
        this.icon = icon;
        this.condition = condition;
    }

    public String getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String getDesc() {
        return desc;
    }

    public String getIcon() {
        return icon;
    }

    /**
     * 是否满足解锁条件
     */
    public boolean isSatisfiedBy(LearningStatsVO stats) {
        return condition.test(stats);
    }
}
