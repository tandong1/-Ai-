-- ============================================
-- 小小学霸系统 - 数据库初始化脚本 v2.0
-- 重要更新：题目表和答题记录表包含答案和解析
-- Version: 2.0
-- Date: 2024-01-15
-- ============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS study_system
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;

USE study_system;

-- ============================================
-- 1. 用户表
-- ============================================
CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50) NOT NULL COMMENT '用户名',
  avatar VARCHAR(255) COMMENT '头像URL',
  total_points INT DEFAULT 0 COMMENT '总积分',
  current_level VARCHAR(20) DEFAULT '初学者' COMMENT '当前等级',
  phone VARCHAR(20) COMMENT '绑定手机号（家长）',
  status VARCHAR(20) DEFAULT 'active' COMMENT '状态: active/inactive',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_phone (phone),
  INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- ============================================
-- 2. 知识库表
-- ============================================
CREATE TABLE knowledge_base (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL COMMENT '用户ID',
  subject VARCHAR(20) NOT NULL COMMENT '科目: math/english/chinese',
  content_type VARCHAR(20) NOT NULL COMMENT '内容类型: image/text',
  content_url VARCHAR(255) COMMENT '图片URL',
  content_text TEXT COMMENT '文本内容',
  extracted_knowledge TEXT COMMENT 'AI提取的知识点',
  status VARCHAR(20) DEFAULT 'active' COMMENT '状态: active/archived',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_user_subject (user_id, subject),
  INDEX idx_status (status),
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='知识库表';

-- ============================================
-- 3. 题目表（包含答案和解析）
-- ============================================
CREATE TABLE questions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL COMMENT '目标用户ID',
  subject VARCHAR(20) NOT NULL COMMENT '科目: math/english/chinese',
  difficulty VARCHAR(20) NOT NULL COMMENT '难度: easy/medium/hard',
  question_type VARCHAR(20) NOT NULL COMMENT '题型: choice/fill',
  question_text TEXT NOT NULL COMMENT '题目内容',
  options JSON COMMENT '选项（选择题）["A选项", "B选项", ...]',
  correct_answer TEXT NOT NULL COMMENT '正确答案（AI生成时提供）',
  analysis TEXT NOT NULL COMMENT '题目解析（AI生成时提供，详细说明解题思路）',
  knowledge_point VARCHAR(255) COMMENT '关联知识点',
  knowledge_base_id BIGINT COMMENT '来源知识库ID',
  generated_date DATE NOT NULL COMMENT '生成日期',
  is_used BOOLEAN DEFAULT FALSE COMMENT '是否已被作答',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_user_date (user_id, generated_date),
  INDEX idx_subject (subject),
  INDEX idx_is_used (is_used),
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (knowledge_base_id) REFERENCES knowledge_base(id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='题目表（含答案和解析）';

-- ============================================
-- 4. 答题记录表（冗余答案和解析）
-- ============================================
CREATE TABLE question_records (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL COMMENT '用户ID',
  question_id BIGINT NOT NULL COMMENT '题目ID',
  subject VARCHAR(20) NOT NULL COMMENT '科目',
  user_answer TEXT COMMENT '用户答案',
  is_correct BOOLEAN NOT NULL COMMENT '是否正确',
  correct_answer TEXT NOT NULL COMMENT '正确答案（冗余，便于历史查看）',
  analysis TEXT NOT NULL COMMENT '解析（冗余，便于历史查看）',
  attempt_count INT DEFAULT 1 COMMENT '尝试次数',
  first_attempt_correct BOOLEAN COMMENT '第一次是否正确',
  time_spent INT COMMENT '答题耗时（秒）',
  answered_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_user_question (user_id, question_id),
  INDEX idx_user_subject (user_id, subject),
  INDEX idx_answered_at (answered_at),
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='答题记录表（冗余答案和解析用于历史回顾）';

-- ============================================
-- 5. 错题本表
-- ============================================
CREATE TABLE wrong_questions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL COMMENT '用户ID',
  question_id BIGINT NOT NULL COMMENT '题目ID',
  subject VARCHAR(20) NOT NULL COMMENT '科目',
  first_wrong_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '首次答错时间',
  wrong_count INT DEFAULT 1 COMMENT '答错次数',
  is_mastered BOOLEAN DEFAULT FALSE COMMENT '是否已掌握',
  mastered_at TIMESTAMP COMMENT '掌握时间',
  last_review_at TIMESTAMP COMMENT '最后复习时间',
  INDEX idx_user_subject (user_id, subject),
  INDEX idx_mastered (is_mastered),
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (question_id) REFERENCES questions(id) ON DELETE CASCADE,
  UNIQUE KEY uk_user_question (user_id, question_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='错题本表';

-- ============================================
-- 6. 积分记录表
-- ============================================
CREATE TABLE points_records (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL COMMENT '用户ID',
  change_type VARCHAR(20) NOT NULL COMMENT '变动类型: earn/consume',
  change_amount INT NOT NULL COMMENT '变动数量',
  source_type VARCHAR(30) NOT NULL COMMENT '来源类型: daily_challenge/exchange/bonus',
  source_id BIGINT COMMENT '来源ID（如答题记录ID、兑换记录ID）',
  description VARCHAR(255) COMMENT '描述',
  balance_after INT NOT NULL COMMENT '变动后余额',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_user_created (user_id, created_at),
  INDEX idx_source (source_type, source_id),
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='积分记录表';

-- ============================================
-- 7. 成就表
-- ============================================
CREATE TABLE achievements (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL COMMENT '用户ID',
  achievement_type VARCHAR(50) NOT NULL COMMENT '成就类型',
  achievement_name VARCHAR(100) NOT NULL COMMENT '成就名称',
  achievement_desc VARCHAR(255) COMMENT '成就描述',
  icon VARCHAR(20) COMMENT '图标emoji',
  unlocked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '解锁时间',
  INDEX idx_user (user_id),
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  UNIQUE KEY uk_user_type (user_id, achievement_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='成就表';

-- ============================================
-- 8. 礼物表
-- ============================================
CREATE TABLE gifts (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(100) NOT NULL COMMENT '礼物名称',
  description TEXT COMMENT '礼物描述',
  points_required INT NOT NULL COMMENT '所需积分',
  images JSON COMMENT '图片URL数组',
  stock INT DEFAULT -1 COMMENT '库存数量（-1表示无限）',
  is_active BOOLEAN DEFAULT TRUE COMMENT '是否上架',
  sort_order INT DEFAULT 0 COMMENT '排序权重',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_active_sort (is_active, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='礼物表';

-- ============================================
-- 9. 兑换记录表
-- ============================================
CREATE TABLE exchange_records (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL COMMENT '用户ID',
  gift_id BIGINT NOT NULL COMMENT '礼物ID',
  gift_name VARCHAR(100) NOT NULL COMMENT '礼物名称（冗余）',
  points_cost INT NOT NULL COMMENT '消耗积分',
  status VARCHAR(20) DEFAULT 'pending' COMMENT '状态: pending/completed/cancelled',
  contact_info VARCHAR(255) COMMENT '联系方式',
  delivery_address TEXT COMMENT '配送地址',
  completed_at TIMESTAMP COMMENT '完成时间',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_user_created (user_id, created_at),
  INDEX idx_status (status),
  FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
  FOREIGN KEY (gift_id) REFERENCES gifts(id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='兑换记录表';

-- ============================================
-- 初始化数据
-- ============================================

-- 插入测试用户
INSERT INTO users (name, avatar, total_points, current_level, phone) VALUES
('大宝', '/images/avatar1.png', 0, '初学者', '13800138000'),
('二宝', '/images/avatar2.png', 0, '初学者', '13800138000');

-- 插入示例题目（包含答案和解析）
INSERT INTO questions (user_id, subject, difficulty, question_type, question_text, options, correct_answer, analysis, knowledge_point, generated_date) VALUES
(1, 'math', 'easy', 'choice',
 '计算: 1/2 + 1/3 = ?',
 '["5/6", "2/5", "1/6", "3/5"]',
 '5/6',
 '解析：要计算 1/2 + 1/3，首先需要通分。\n步骤1：找最小公倍数，2和3的最小公倍数是6\n步骤2：1/2 = 3/6\n步骤3：1/3 = 2/6\n步骤4：3/6 + 2/6 = 5/6\n因此答案是 5/6。',
 '分数加法-通分',
 CURDATE()),

(1, 'math', 'medium', 'fill',
 '一个长方形的长是8cm，宽是5cm，它的周长是____cm。',
 NULL,
 '26',
 '解析：长方形周长公式是 (长+宽)×2\n代入数值：(8+5)×2 = 13×2 = 26cm\n易错点：有些同学可能只算了两条边的和(8+5=13)，忘记乘以2。',
 '长方形周长',
 CURDATE()),

(2, 'english', 'easy', 'choice',
 'What is the capital of France?',
 '["London", "Berlin", "Paris", "Madrid"]',
 'Paris',
 'Analysis: Paris is the capital and largest city of France. \n- London is the capital of the UK\n- Berlin is the capital of Germany\n- Madrid is the capital of Spain\nRemember: France = Paris (both start with different letters, so don''t get confused!)',
 'World capitals',
 CURDATE());

-- 插入礼物数据
INSERT INTO gifts (name, description, points_required, images, stock, sort_order) VALUES
('乐高积木玩具', '激发创造力，拼搭快乐时光', 500,
  '["https://via.placeholder.com/600x400/4299e1/ffffff?text=乐高1", "https://via.placeholder.com/600x400/3b82f6/ffffff?text=乐高2", "https://via.placeholder.com/600x400/2563eb/ffffff?text=乐高3"]',
  10, 1),
('儿童故事书套装', '10本精选绘本，开启阅读之旅', 300,
  '["https://via.placeholder.com/600x400/10b981/ffffff?text=故事书1", "https://via.placeholder.com/600x400/059669/ffffff?text=故事书2", "https://via.placeholder.com/600x400/047857/ffffff?text=故事书3"]',
  20, 2),
('儿童智能手表', '定位、通话、计步，安全又有趣', 800,
  '["https://via.placeholder.com/600x400/f59e0b/ffffff?text=手表1", "https://via.placeholder.com/600x400/d97706/ffffff?text=手表2", "https://via.placeholder.com/600x400/b45309/ffffff?text=手表3"]',
  5, 3),
('科学实验套装', '20+趣味实验，探索科学奥秘', 400,
  '["https://via.placeholder.com/600x400/8b5cf6/ffffff?text=实验1", "https://via.placeholder.com/600x400/7c3aed/ffffff?text=实验2", "https://via.placeholder.com/600x400/6d28d9/ffffff?text=实验3"]',
  15, 4),
('儿童滑板车', '可折叠设计，户外运动好伙伴', 600,
  '["https://via.placeholder.com/600x400/ec4899/ffffff?text=滑板车1", "https://via.placeholder.com/600x400/db2777/ffffff?text=滑板车2", "https://via.placeholder.com/600x400/be185d/ffffff?text=滑板车3"]',
  8, 5),
('超级飞侠玩具', '变形机器人，陪伴成长每一天', 350,
  '["https://via.placeholder.com/600x400/ef4444/ffffff?text=飞侠1", "https://via.placeholder.com/600x400/dc2626/ffffff?text=飞侠2", "https://via.placeholder.com/600x400/b91c1c/ffffff?text=飞侠3"]',
  12, 6);

-- ============================================
-- 查看表结构
-- ============================================
SHOW TABLES;

-- 查看题目表结构（验证包含答案和解析字段）
DESCRIBE questions;

-- 查看答题记录表结构（验证冗余答案和解析字段）
DESCRIBE question_records;
