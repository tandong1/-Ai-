# 小小学霸系统技术规格文档 (Spec v2.0)

## 1. 系统概述

### 1.1 项目背景
为两个外甥设计的学习小程序，支持每日自动生成个性化题目，AI辅助批改，积分激励体系。

### 1.2 核心功能
- 每日凌晨自动生成个性化题目（每个用户独立题库）
- **题目生成时包含答案和解析**
- 基于上传的知识点/图片生成题目
- AI批改答案并提供解析
- **答错时展示正确答案和解析**
- 错题自动标记与管理
- 积分体系与礼物兑换

### 1.3 技术栈
- **前端**: 微信小程序
- **后端**: Node.js + Express / Python + FastAPI
- **数据库**: MySQL / PostgreSQL
- **AI服务**: 调用大模型API（Claude/GPT-4/通义千问等）
- **定时任务**: node-cron / APScheduler
- **图片存储**: 阿里云OSS / 腾讯云COS

---

## 2. 系统架构

### 2.1 整体架构图
```
┌─────────────────────────────────────────────────────────┐
│                      微信小程序                          │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐             │
│  │科目选择  │  │  答题    │  │积分商城  │             │
│  └──────────┘  └──────────┘  └──────────┘             │
└───────────────────────┬─────────────────────────────────┘
                        │ HTTPS
                        ▼
┌─────────────────────────────────────────────────────────┐
│                    后端API服务                           │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐       │
│  │用户管理    │  │题目管理    │  │积分管理    │       │
│  └────────────┘  └────────────┘  └────────────┘       │
│  ┌────────────┐  ┌────────────┐  ┌────────────┐       │
│  │答题记录    │  │错题管理    │  │兑换管理    │       │
│  └────────────┘  └────────────┘  └────────────┘       │
└───────────┬─────────────────────────────┬───────────────┘
            │                             │
            ▼                             ▼
┌─────────────────────┐     ┌─────────────────────────┐
│   MySQL数据库        │     │   大模型API服务          │
│  - 用户数据          │     │  - 题目生成(含答案+解析) │
│  - 题目库(含解析)    │     │  - 答案批改              │
│  - 答题记录         │     │  - 知识点提取            │
│  - 积分记录         │     └─────────────────────────┘
└─────────────────────┘
            ▲
            │
┌─────────────────────┐
│   定时任务服务       │
│  - 每日00:00生成题目 │
│  - 积分统计          │
└─────────────────────┘
```

### 2.2 业务流程

#### 2.2.1 知识点上传流程
```
用户 → 上传图片/文本 → 保存到OSS → 调用大模型提取知识点 
   → 存入知识库表 → 关联到用户
```

#### 2.2.2 每日出题流程（含答案和解析）
```
定时任务(00:00) → 遍历所有用户 → 读取用户知识库 
   → 调用大模型生成题目 → AI返回：题目+选项+答案+解析
   → 保存到题目表(包含correct_answer和analysis字段)
   → 标记为当日题目
```

#### 2.2.3 答题流程（答错展示答案和解析）
```
用户选择科目 → 获取今日题目(不含答案和解析) → 作答 → 提交答案 
   → 后端对比答案判断对错 → 返回结果
   ├─ 答对：返回"正确" + 解析
   └─ 答错：返回"错误" + 正确答案 + 解析 + 标记入错题本
   → 计算积分 → 更新用户积分
```

---

## 3. 数据库设计

### 3.1 用户表 (users)
```sql
CREATE TABLE users (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  name VARCHAR(50) NOT NULL COMMENT '用户名',
  avatar VARCHAR(255) COMMENT '头像URL',
  total_points INT DEFAULT 0 COMMENT '总积分',
  current_level VARCHAR(20) DEFAULT '初学者' COMMENT '当前等级',
  phone VARCHAR(20) COMMENT '绑定手机号（家长）',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_phone (phone)
) COMMENT='用户表';
```

### 3.2 知识库表 (knowledge_base)
```sql
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
  FOREIGN KEY (user_id) REFERENCES users(id)
) COMMENT='知识库表';
```

### 3.3 题目表 (questions) **重点修改**
```sql
CREATE TABLE questions (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL COMMENT '目标用户ID',
  subject VARCHAR(20) NOT NULL COMMENT '科目: math/english/chinese',
  difficulty VARCHAR(20) NOT NULL COMMENT '难度: easy/medium/hard',
  question_type VARCHAR(20) NOT NULL COMMENT '题型: choice/fill',
  question_text TEXT NOT NULL COMMENT '题目内容',
  options JSON COMMENT '选项（选择题）["A选项", "B选项", ...]',
  correct_answer TEXT NOT NULL COMMENT '正确答案（生成时由AI提供）',
  analysis TEXT NOT NULL COMMENT '题目解析（生成时由AI提供，详细说明解题思路）',
  knowledge_point VARCHAR(255) COMMENT '关联知识点',
  knowledge_base_id BIGINT COMMENT '来源知识库ID',
  generated_date DATE NOT NULL COMMENT '生成日期',
  is_used BOOLEAN DEFAULT FALSE COMMENT '是否已被作答',
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_user_date (user_id, generated_date),
  INDEX idx_subject (subject),
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (knowledge_base_id) REFERENCES knowledge_base(id)
) COMMENT='题目表（含答案和解析）';
```

### 3.4 答题记录表 (question_records) **重点修改**
```sql
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
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (question_id) REFERENCES questions(id)
) COMMENT='答题记录表（冗余答案和解析用于历史回顾）';
```

### 3.5 错题本表 (wrong_questions)
```sql
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
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (question_id) REFERENCES questions(id),
  UNIQUE KEY uk_user_question (user_id, question_id)
) COMMENT='错题本表';
```

### 3.6 积分记录表 (points_records)
```sql
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
  FOREIGN KEY (user_id) REFERENCES users(id)
) COMMENT='积分记录表';
```

### 3.7 成就表 (achievements)
```sql
CREATE TABLE achievements (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  user_id BIGINT NOT NULL COMMENT '用户ID',
  achievement_type VARCHAR(50) NOT NULL COMMENT '成就类型',
  achievement_name VARCHAR(100) NOT NULL COMMENT '成就名称',
  achievement_desc VARCHAR(255) COMMENT '成就描述',
  icon VARCHAR(20) COMMENT '图标emoji',
  unlocked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP COMMENT '解锁时间',
  INDEX idx_user (user_id),
  FOREIGN KEY (user_id) REFERENCES users(id),
  UNIQUE KEY uk_user_type (user_id, achievement_type)
) COMMENT='成就表';
```

### 3.8 礼物表 (gifts)
```sql
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
) COMMENT='礼物表';
```

### 3.9 兑换记录表 (exchange_records)
```sql
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
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (gift_id) REFERENCES gifts(id)
) COMMENT='兑换记录表';
```

---

## 4. API接口设计

### 4.1 用户相关

#### 4.1.1 用户列表
```
GET /api/users
Response: {
  "code": 200,
  "data": [
    {
      "id": 1,
      "name": "大宝",
      "avatar": "https://...",
      "totalPoints": 350
    }
  ]
}
```

#### 4.1.2 用户登录/选择
```
POST /api/users/login
Body: { "userId": 1 }
Response: {
  "code": 200,
  "data": {
    "user": {...},
    "token": "jwt_token"
  }
}
```

### 4.2 知识库相关

#### 4.2.1 上传知识点
```
POST /api/knowledge/upload
Headers: Authorization: Bearer <token>
Body (multipart): {
  "subject": "math",
  "contentType": "image",
  "file": <binary>
}
Response: {
  "code": 200,
  "data": {
    "id": 123,
    "contentUrl": "https://...",
    "extractedKnowledge": "关于分数加减法..."
  }
}
```

#### 4.2.2 知识点列表
```
GET /api/knowledge?subject=math&page=1&pageSize=10
Response: {
  "code": 200,
  "data": {
    "list": [...],
    "total": 25
  }
}
```

### 4.3 题目相关

#### 4.3.1 获取今日题目 **（不返回答案和解析）**
```
GET /api/questions/daily?subject=math
Response: {
  "code": 200,
  "data": {
    "subject": "math",
    "questions": [
      {
        "id": 456,
        "questionType": "choice",
        "questionText": "计算: 1/2 + 1/3 = ?",
        "options": ["5/6", "2/5", "1/6", "3/5"],
        "difficulty": "easy"
        // 注意：不返回 correctAnswer 和 analysis
      }
    ]
  }
}
```

#### 4.3.2 提交答案 **（答错时返回答案和解析）**
```
POST /api/questions/submit
Body: {
  "subject": "math",
  "answers": [
    {
      "questionId": 456,
      "userAnswer": "2/5"  // 假设答错
    }
  ]
}
Response: {
  "code": 200,
  "data": {
    "results": [
      {
        "questionId": 456,
        "isCorrect": false,
        "userAnswer": "2/5",
        "correctAnswer": "5/6",  // 返回正确答案
        "analysis": "解析：要计算 1/2 + 1/3，首先需要通分。\n1/2 = 3/6\n1/3 = 2/6\n所以 3/6 + 2/6 = 5/6",  // 返回详细解析
        "pointsEarned": 0
      }
    ],
    "totalPointsEarned": 0,
    "newBalance": 350
  }
}

// 如果答对的响应：
Response: {
  "code": 200,
  "data": {
    "results": [
      {
        "questionId": 456,
        "isCorrect": true,
        "userAnswer": "5/6",
        "correctAnswer": "5/6",
        "analysis": "解析：要计算 1/2 + 1/3，首先需要通分。\n1/2 = 3/6\n1/3 = 2/6\n所以 3/6 + 2/6 = 5/6",  // 答对也返回解析
        "pointsEarned": 10
      }
    ],
    "totalPointsEarned": 10,
    "newBalance": 360
  }
}
```

### 4.4 错题本相关

#### 4.4.1 错题列表 **（包含答案和解析）**
```
GET /api/wrong-questions?subject=math&page=1
Response: {
  "code": 200,
  "data": {
    "list": [
      {
        "id": 789,
        "question": {
          "questionText": "计算: 1/2 + 1/3 = ?",
          "options": ["5/6", "2/5", "1/6", "3/5"],
          "correctAnswer": "5/6",  // 包含答案
          "analysis": "解析：..."  // 包含解析
        },
        "userAnswer": "2/5",
        "wrongCount": 2,
        "firstWrongAt": "2024-01-15",
        "isMastered": false
      }
    ]
  }
}
```

### 4.5 积分相关

#### 4.5.1 积分明细
```
GET /api/points/records?page=1
Response: {
  "code": 200,
  "data": {
    "list": [
      {
        "changeType": "earn",
        "changeAmount": 50,
        "sourceType": "daily_challenge",
        "description": "数学挑战完成",
        "balanceAfter": 400,
        "createdAt": "2024-01-15 10:30:00"
      }
    ]
  }
}
```

### 4.6 商城相关

#### 4.6.1 礼物列表
```
GET /api/gifts
Response: {
  "code": 200,
  "data": [
    {
      "id": 1,
      "name": "乐高积木",
      "description": "...",
      "pointsRequired": 500,
      "images": ["url1", "url2"],
      "stock": 10
    }
  ]
}
```

#### 4.6.2 兑换礼物
```
POST /api/exchange
Body: {
  "giftId": 1,
  "contactInfo": "13800138000",
  "deliveryAddress": "..."
}
Response: {
  "code": 200,
  "data": {
    "exchangeId": 123,
    "newBalance": 0
  }
}
```

---

## 5. 定时任务设计

### 5.1 每日生成题目 (Daily Question Generation) **含答案和解析**

#### 5.1.1 执行时间
```
Cron: 0 0 * * * (每天凌晨00:00)
```

#### 5.1.2 执行逻辑
```javascript
async function generateDailyQuestions() {
  // 1. 获取所有活跃用户
  const users = await db.users.findAll({ status: 'active' });
  
  for (const user of users) {
    // 2. 为每个科目生成题目
    for (const subject of ['math', 'english', 'chinese']) {
      // 3. 获取用户的知识库
      const knowledge = await db.knowledgeBase.findAll({
        userId: user.id,
        subject: subject,
        status: 'active'
      });
      
      // 4. 构建prompt，要求AI返回答案和解析
      const prompt = buildPrompt(user, subject, knowledge);
      
      // 5. 调用大模型生成题目（含答案和解析）
      const questions = await callAI(prompt);
      
      // 6. 保存题目到数据库（包含correct_answer和analysis）
      await saveQuestions(user.id, subject, questions);
    }
  }
}

function buildPrompt(user, subject, knowledge) {
  return `
    为学生"${user.name}"生成5道${subject}题目。
    
    知识背景：
    ${knowledge.map(k => k.extractedKnowledge).join('\n')}
    
    要求：
    1. 题目难度适中（${user.currentLevel}水平）
    2. 包含3道选择题、2道填空题
    3. 每道题**必须包含**：
       - 题目内容 (questionText)
       - 选项 (options, 仅选择题)
       - 正确答案 (correctAnswer)
       - 详细解析 (analysis) - 包含解题思路、知识点说明、计算步骤
    4. 题目要有针对性，基于提供的知识背景
    5. 解析要详细，便于学生理解错误原因
    6. 返回JSON格式
    
    JSON格式示例：
    [
      {
        "questionType": "choice",
        "questionText": "计算: 1/2 + 1/3 = ?",
        "options": ["5/6", "2/5", "1/6", "3/5"],
        "correctAnswer": "5/6",
        "analysis": "解析：要计算 1/2 + 1/3，首先需要通分。\n步骤1：找最小公倍数，2和3的最小公倍数是6\n步骤2：1/2 = 3/6\n步骤3：1/3 = 2/6\n步骤4：3/6 + 2/6 = 5/6\n因此答案是 5/6。",
        "difficulty": "easy",
        "knowledgePoint": "分数加法-通分"
      },
      {
        "questionType": "fill",
        "questionText": "一个长方形的长是8cm，宽是5cm，它的周长是____cm。",
        "correctAnswer": "26",
        "analysis": "解析：长方形周长公式是 (长+宽)×2\n代入数值：(8+5)×2 = 13×2 = 26cm",
        "difficulty": "medium",
        "knowledgePoint": "长方形周长"
      }
    ]
  `;
}
```

#### 5.1.3 保存题目逻辑
```javascript
async function saveQuestions(userId, subject, questions) {
  const today = new Date().toISOString().split('T')[0];
  
  for (const q of questions) {
    await db.questions.create({
      userId: userId,
      subject: subject,
      difficulty: q.difficulty,
      questionType: q.questionType,
      questionText: q.questionText,
      options: q.options ? JSON.stringify(q.options) : null,
      correctAnswer: q.correctAnswer,  // 保存答案
      analysis: q.analysis,             // 保存解析
      knowledgePoint: q.knowledgePoint,
      generatedDate: today,
      isUsed: false
    });
  }
}
```

---

## 6. 大模型集成方案

### 6.1 功能模块

#### 6.1.1 题目生成（含答案和解析）
```javascript
async function generateQuestions(prompt) {
  const response = await fetch('https://api.anthropic.com/v1/messages', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'x-api-key': process.env.ANTHROPIC_API_KEY,
      'anthropic-version': '2023-06-01'
    },
    body: JSON.stringify({
      model: 'claude-3-5-sonnet-20241022',
      max_tokens: 4096,
      messages: [{ role: 'user', content: prompt }]
    })
  });
  
  const result = await response.json();
  
  // 解析AI返回的JSON格式题目
  const content = result.content[0].text;
  const questions = JSON.parse(content);
  
  // 验证每道题都包含答案和解析
  questions.forEach(q => {
    if (!q.correctAnswer || !q.analysis) {
      throw new Error('题目缺少答案或解析');
    }
  });
  
  return questions;
}
```

#### 6.1.2 答案批改（简化版，直接对比）
```javascript
async function gradeAnswer(question, userAnswer) {
  // 直接对比答案（简单高效）
  const isCorrect = normalizeAnswer(userAnswer) === normalizeAnswer(question.correctAnswer);
  
  return {
    isCorrect: isCorrect,
    correctAnswer: question.correctAnswer,
    analysis: question.analysis,  // 直接使用数据库中的解析
    pointsEarned: isCorrect ? 10 : 0
  };
}

function normalizeAnswer(answer) {
  // 标准化答案（去空格、转小写等）
  return answer.toString().trim().toLowerCase();
}
```

#### 6.1.3 知识点提取
```javascript
async function extractKnowledge(imageUrl, text) {
  const prompt = `
    从以下内容中提取学习知识点：
    ${text || '图片：' + imageUrl}
    
    提取要点：
    1. 识别学科领域
    2. 提炼核心知识点
    3. 标注难度等级
    4. 建议练习方向
  `;
  
  return await callAI(prompt);
}
```

---

## 7. 前端展示逻辑

### 7.1 答题结果展示

#### 7.1.1 答对时
```javascript
// 前端收到响应
{
  "isCorrect": true,
  "userAnswer": "5/6",
  "correctAnswer": "5/6",
  "analysis": "解析：...",
  "pointsEarned": 10
}

// 展示：
// ✅ 回答正确！+10分
// 📝 解析：要计算 1/2 + 1/3，首先需要通分...
```

#### 7.1.2 答错时
```javascript
// 前端收到响应
{
  "isCorrect": false,
  "userAnswer": "2/5",
  "correctAnswer": "5/6",  // 展示正确答案
  "analysis": "解析：...",  // 展示详细解析
  "pointsEarned": 0
}

// 展示：
// ❌ 回答错误
// 你的答案：2/5
// 正确答案：5/6
// 📝 解析：要计算 1/2 + 1/3，首先需要通分...
```

---

## 8. 数据流转图

```
知识点上传 → 知识库表
              ↓
        定时任务(00:00)
              ↓
    调用AI生成题目(含答案+解析)
              ↓
   题目表(保存correct_answer + analysis)
              ↓
        用户答题
              ↓
      提交答案到后端
              ↓
      后端对比correct_answer
              ↓
        判断对错
         ↙    ↘
      正确     错误
       ↓        ↓
返回解析    返回答案+解析
       ↓        ↓
    加积分   错题本表(关联question_id可获取解析)
       ↓        ↓
答题记录表  答题记录表(冗余答案+解析)
  (冗余)
```

---

## 9. 关键改进点总结

### 9.1 题目生成阶段
✅ AI生成题目时**必须包含答案和解析**
✅ 保存到数据库的`correct_answer`和`analysis`字段
✅ 解析要详细，包含解题思路和步骤

### 9.2 答题阶段
✅ 获取题目时**不返回答案和解析**（防止作弊）
✅ 提交答案后，后端对比`correct_answer`
✅ 答错时**返回正确答案和解析**
✅ 答对时也返回解析（巩固理解）

### 9.3 错题本阶段
✅ 错题记录关联`question_id`
✅ 可随时查看题目的答案和解析
✅ 复习时再次作答，依然提供反馈

### 9.4 数据冗余设计
✅ `question_records`表冗余`correct_answer`和`analysis`
✅ 原因：便于历史回顾，即使题目被删除也能查看
✅ 兑换记录表冗余`gift_name`同理

---

## 10. 实施建议

### 10.1 第一阶段（MVP）
- ✅ 完成数据库设计并建表
- ✅ 实现用户管理API
- ✅ 实现题目管理API（手动录入，含答案和解析）
- ✅ 实现答题流程（前端提交，后端对比答案，返回解析）
- ✅ 实现积分系统
- ✅ 实现商城兑换

### 10.2 第二阶段（AI集成）
- ✅ 集成大模型API
- ✅ 实现知识点上传和提取
- ✅ 实现AI生成题目（含答案和解析）
- ✅ 优化题目生成prompt

### 10.3 第三阶段（自动化）
- ✅ 实现定时任务
- ✅ 实现错题本自动管理
- ✅ 实现学习报告生成

### 10.4 第四阶段（优化）
- ✅ 题目质量优化
- ✅ 个性化推荐算法
- ✅ 家长端功能（查看学习进度）
- ✅ 数据分析与可视化

---

## 11. 成本预估

### 11.1 云服务
- 服务器：¥100/月（腾讯云轻量服务器）
- 数据库：¥50/月（MySQL）
- 对象存储：¥20/月（100GB）

### 11.2 AI服务
- Claude API：约¥0.015/1K tokens
- 每道题约500 tokens（含答案和解析）
- 每天生成15道题（5题×3科目×2用户）= 7.5K tokens
- 日成本：约¥0.11
- 月成本：约¥3.3

### 11.3 总成本
约 ¥173/月

---

## 附录：环境变量配置

```bash
# .env
DATABASE_URL=mysql://user:pass@localhost:3306/study
ANTHROPIC_API_KEY=sk-ant-xxx
OSS_ACCESS_KEY_ID=xxx
OSS_ACCESS_KEY_SECRET=xxx
OSS_BUCKET=study-resources
JWT_SECRET=your_secret_key
```

---

## 附录：Prompt模板示例

### 生成题目Prompt（完整版）
```
你是一位资深的小学教师，擅长根据学生的学习情况出题。

学生信息：
- 姓名：大宝
- 当前等级：学习者
- 科目：数学

知识背景：
1. 最近学习了分数的加减运算
2. 掌握了通分和约分的基本方法
3. 对应用题理解较好

任务：
生成5道数学题目，包含3道选择题和2道填空题。

要求：
1. 难度适中，符合"学习者"水平
2. 每道题必须包含：
   - questionText: 题目内容
   - questionType: "choice" 或 "fill"
   - options: 选项数组（仅选择题需要）
   - correctAnswer: 正确答案
   - analysis: 详细解析（200字以内）
   - difficulty: "easy", "medium", 或 "hard"
   - knowledgePoint: 关联知识点
3. 解析要详细说明：
   - 解题思路
   - 计算步骤
   - 易错点提醒
4. 返回严格的JSON格式

返回格式：
[
  {
    "questionType": "choice",
    "questionText": "...",
    "options": ["A", "B", "C", "D"],
    "correctAnswer": "A",
    "analysis": "...",
    "difficulty": "easy",
    "knowledgePoint": "..."
  }
]
```
