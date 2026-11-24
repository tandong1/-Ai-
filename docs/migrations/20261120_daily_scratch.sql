-- 在 study_system 数据库中执行；可重复执行，不覆盖已有概率配置。
CREATE TABLE IF NOT EXISTS scratch_config (
  id INT PRIMARY KEY,
  win_probability INT NOT NULL DEFAULT 3000 COMMENT '万分比：0不中奖，3000为30%，10000必中',
  reward_points INT NOT NULL DEFAULT 1,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  CHECK (win_probability BETWEEN 0 AND 10000),
  CHECK (reward_points BETWEEN 1 AND 10000)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
INSERT IGNORE INTO scratch_config (id, win_probability, reward_points) VALUES (1, 3000, 1);

CREATE TABLE IF NOT EXISTS daily_scratch_rewards (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL,
  reward_date DATE NOT NULL,
  points INT NOT NULL,
  message VARCHAR(255) NOT NULL,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_user_date (user_id, reward_date),
  FOREIGN KEY (user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

-- 示例：修改后下一次首次开奖立即生效，已开奖结果不变。
-- UPDATE scratch_config SET win_probability = 5000, reward_points = 2 WHERE id = 1;
