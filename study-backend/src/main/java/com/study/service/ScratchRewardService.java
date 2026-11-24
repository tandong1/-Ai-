package com.study.service;

import com.study.common.BusinessException;
import com.study.mapper.QuestionRecordMapper;
import com.study.vo.ScratchRewardVO;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ScratchRewardService {
    private final JdbcTemplate jdbc;
    private final QuestionRecordMapper records;
    private final AchievementService achievements;
    private final SecureRandom random = new SecureRandom();
    private static final List<String> SUBJECTS = List.of("math", "english", "chinese");

    @Transactional(readOnly = true)
    public ScratchRewardVO status(Long userId) {
        LocalDate date = today();
        Integer balance = jdbc.queryForObject("SELECT total_points FROM users WHERE id = ?", Integer.class, userId);
        return load(userId, date, balance == null ? 0 : balance);
    }

    @Transactional(rollbackFor = Exception.class)
    public ScratchRewardVO reveal(Long userId, LocalDate requestedDate) {
        // 锁同一用户，串行处理重复请求；唯一键作为第二道保障。
        List<Integer> balances = jdbc.query("SELECT total_points FROM users WHERE id = ? FOR UPDATE",
                (rs, row) -> rs.getInt(1), userId);
        if (balances.isEmpty()) throw new BusinessException("用户不存在");
        LocalDate date = today();
        if (!date.equals(requestedDate)) throw new BusinessException("日期已更新，请刷新后领取今日奖励");
        ScratchRewardVO result = load(userId, date, balances.get(0));
        if (result.isRevealed()) return result;
        if (!result.isEligible()) throw new BusinessException("完成今日三科任务后才能刮奖");

        Map<String, Object> config = jdbc.queryForMap(
                "SELECT win_probability, reward_points FROM scratch_config WHERE id = 1");
        int probability = ((Number) config.get("win_probability")).intValue();
        int reward = ((Number) config.get("reward_points")).intValue();
        if (probability < 0 || probability > 10000 || reward < 1 || reward > 10000)
            throw new BusinessException("刮奖配置暂不可用，请稍后再试");
        int points = random.nextInt(10000) < probability ? reward : 0;
        List<String> messages = jdbc.queryForList(
                "SELECT message FROM scratch_messages WHERE enabled = 1 AND CHAR_LENGTH(TRIM(message)) > 0 ORDER BY id",
                String.class);
        if (messages.isEmpty()) throw new BusinessException("鼓励语暂未配置，请稍后再试");
        String message = messages.get(random.nextInt(messages.size()));
        jdbc.update("INSERT INTO daily_scratch_rewards (user_id, reward_date, points, message) VALUES (?, ?, ?, ?)",
                userId, date, points, message);
        if (points > 0) {
            jdbc.update("UPDATE users SET total_points = total_points + ?, updated_at = NOW() WHERE id = ?", points, userId);
            jdbc.update("INSERT INTO points_records (user_id, change_type, change_amount, source_type, description, balance_after) " +
                    "VALUES (?, 'earn', ?, 'bonus', '每日三科完成刮奖奖励', ?)", userId, points, balances.get(0) + points);
            achievements.evaluateAndUnlock(userId);
        }
        return load(userId, date, balances.get(0) + points);
    }

    private LocalDate today() {
        // 与现有今日完成状态统一使用数据库日期。
        return jdbc.queryForObject("SELECT CURDATE()", (rs, row) -> rs.getDate(1).toLocalDate());
    }

    private ScratchRewardVO load(Long userId, LocalDate date, int balance) {
        ScratchRewardVO result = new ScratchRewardVO();
        result.setRewardDate(date.toString());
        result.setTotalPoints(balance);
        result.setEligible(records.selectCompletedSubjects(userId, date).containsAll(SUBJECTS));
        List<Map<String, Object>> rewards = jdbc.queryForList(
                "SELECT points, message FROM daily_scratch_rewards WHERE user_id = ? AND reward_date = ?", userId, date);
        if (!rewards.isEmpty()) {
            result.setRevealed(true);
            result.setEligible(true);
            result.setPoints(((Number) rewards.get(0).get("points")).intValue());
            result.setMessage((String) rewards.get(0).get("message"));
        }
        return result;
    }
}
