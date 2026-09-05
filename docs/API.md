# API 接口文档

## 基础信息

### Base URL
```
开发环境: http://localhost:3000/api
生产环境: https://api.study.example.com/api
```

### 通用响应格式
```json
{
  "code": 200,          // 状态码
  "message": "success", // 消息
  "data": {...}         // 数据
}
```

### 错误码
- `200`: 成功
- `400`: 请求参数错误
- `401`: 未授权
- `403`: 无权限
- `404`: 资源不存在
- `500`: 服务器错误

---

## 认证

### 登录/选择用户
```http
POST /users/login
```

**请求体**
```json
{
  "userId": 1
}
```

**响应**
```json
{
  "code": 200,
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "user": {
      "id": 1,
      "name": "郭坤铭",
      "avatar": "/images/avatar1.png",
      "totalPoints": 350,
      "currentLevel": "学习者"
    }
  }
}
```

### 后续请求
在请求头中携带token：
```http
Authorization: Bearer <token>
```

---

## 知识库管理

### 上传知识点
```http
POST /knowledge/upload
Content-Type: multipart/form-data
Authorization: Bearer <token>
```

**请求参数**
- `subject`: string (required) - 科目 (math/english/chinese)
- `contentType`: string (required) - 类型 (image/text)
- `file`: file (optional) - 图片文件
- `text`: string (optional) - 文本内容

**响应**
```json
{
  "code": 200,
  "data": {
    "id": 123,
    "contentUrl": "https://oss.example.com/images/123.jpg",
    "extractedKnowledge": "本次上传的内容涉及分数的加减运算，包括通分、约分等基础知识点...",
    "subject": "math"
  }
}
```

### 获取知识库列表
```http
GET /knowledge?subject=math&page=1&pageSize=10
Authorization: Bearer <token>
```

**查询参数**
- `subject`: string (optional) - 科目筛选
- `page`: number (default: 1) - 页码
- `pageSize`: number (default: 10) - 每页数量

**响应**
```json
{
  "code": 200,
  "data": {
    "list": [
      {
        "id": 123,
        "subject": "math",
        "contentType": "image",
        "contentUrl": "https://...",
        "extractedKnowledge": "...",
        "createdAt": "2024-01-15T10:30:00Z"
      }
    ],
    "total": 25,
    "page": 1,
    "pageSize": 10
  }
}
```

---

## 题目管理

### 获取今日题目
```http
GET /questions/daily?subject=math
Authorization: Bearer <token>
```

**查询参数**
- `subject`: string (required) - 科目 (math/english/chinese)

**响应**
```json
{
  "code": 200,
  "data": {
    "subject": "math",
    "generatedDate": "2024-01-15",
    "questions": [
      {
        "id": 456,
        "questionType": "choice",
        "questionText": "计算: 1/2 + 1/3 = ?",
        "options": ["5/6", "2/5", "1/6", "3/5"],
        "difficulty": "easy",
        "knowledgePoint": "分数加法"
      },
      {
        "id": 457,
        "questionType": "fill",
        "questionText": "一个长方形的长是8cm，宽是5cm，它的周长是____cm。",
        "difficulty": "medium",
        "knowledgePoint": "周长计算"
      }
    ]
  }
}
```

### 提交答案
```http
POST /questions/submit
Authorization: Bearer <token>
```

**请求体**
```json
{
  "subject": "math",
  "answers": [
    {
      "questionId": 456,
      "userAnswer": "5/6"
    },
    {
      "questionId": 457,
      "userAnswer": "26"
    }
  ]
}
```

**响应**
```json
{
  "code": 200,
  "data": {
    "results": [
      {
        "questionId": 456,
        "isCorrect": true,
        "correctAnswer": "5/6",
        "userAnswer": "5/6",
        "aiFeedback": "回答正确！解析：1/2 = 3/6, 1/3 = 2/6, 所以 3/6 + 2/6 = 5/6",
        "pointsEarned": 10
      },
      {
        "questionId": 457,
        "isCorrect": true,
        "correctAnswer": "26",
        "userAnswer": "26",
        "aiFeedback": "完全正确！周长公式：(长+宽)×2 = (8+5)×2 = 26cm",
        "pointsEarned": 10
      }
    ],
    "totalPointsEarned": 20,
    "correctCount": 2,
    "totalCount": 2,
    "accuracy": 100,
    "newBalance": 370
  }
}
```

### 获取历史题目
```http
GET /questions/history?subject=math&page=1&pageSize=20
Authorization: Bearer <token>
```

**响应**
```json
{
  "code": 200,
  "data": {
    "list": [
      {
        "id": 456,
        "subject": "math",
        "questionText": "...",
        "userAnswer": "5/6",
        "isCorrect": true,
        "answeredAt": "2024-01-15T10:45:00Z"
      }
    ],
    "total": 156
  }
}
```

---

## 错题本

### 获取错题列表
```http
GET /wrong-questions?subject=math&page=1
Authorization: Bearer <token>
```

**查询参数**
- `subject`: string (optional) - 科目筛选
- `isMastered`: boolean (optional) - 是否已掌握
- `page`: number (default: 1)
- `pageSize`: number (default: 20)

**响应**
```json
{
  "code": 200,
  "data": {
    "list": [
      {
        "id": 789,
        "questionId": 458,
        "subject": "math",
        "question": {
          "questionText": "...",
          "correctAnswer": "...",
          "analysis": "..."
        },
        "wrongCount": 2,
        "firstWrongAt": "2024-01-10T10:30:00Z",
        "lastReviewAt": "2024-01-12T14:20:00Z",
        "isMastered": false
      }
    ],
    "total": 15,
    "masteredCount": 8,
    "pendingCount": 7
  }
}
```

### 标记为已掌握
```http
PUT /wrong-questions/:id/master
Authorization: Bearer <token>
```

**响应**
```json
{
  "code": 200,
  "message": "已标记为掌握"
}
```

---

## 积分系统

### 获取积分明细
```http
GET /points/records?page=1&pageSize=20
Authorization: Bearer <token>
```

**查询参数**
- `changeType`: string (optional) - 类型筛选 (earn/consume)
- `startDate`: string (optional) - 开始日期 (YYYY-MM-DD)
- `endDate`: string (optional) - 结束日期

**响应**
```json
{
  "code": 200,
  "data": {
    "currentBalance": 370,
    "list": [
      {
        "id": 1001,
        "changeType": "earn",
        "changeAmount": 20,
        "sourceType": "daily_challenge",
        "description": "数学挑战完成",
        "balanceAfter": 370,
        "createdAt": "2024-01-15T11:00:00Z"
      },
      {
        "id": 1000,
        "changeType": "consume",
        "changeAmount": -300,
        "sourceType": "exchange",
        "description": "兑换：儿童故事书套装",
        "balanceAfter": 350,
        "createdAt": "2024-01-14T15:30:00Z"
      }
    ],
    "total": 45
  }
}
```

### 获取成就列表
```http
GET /achievements
Authorization: Bearer <token>
```

**响应**
```json
{
  "code": 200,
  "data": {
    "achievements": [
      {
        "id": 1,
        "achievementType": "first_challenge",
        "achievementName": "首战告捷",
        "achievementDesc": "完成第一次挑战",
        "icon": "🎯",
        "unlocked": true,
        "unlockedAt": "2024-01-10T09:15:00Z"
      },
      {
        "id": 2,
        "achievementType": "continuous_3_days",
        "achievementName": "坚持不懈",
        "achievementDesc": "连续学习3天",
        "icon": "🔥",
        "unlocked": false,
        "unlockedAt": null
      }
    ],
    "unlockedCount": 1,
    "totalCount": 6
  }
}
```

---

## 积分商城

### 获取礼物列表
```http
GET /gifts
Authorization: Bearer <token>
```

**响应**
```json
{
  "code": 200,
  "data": [
    {
      "id": 1,
      "name": "乐高积木玩具",
      "description": "激发创造力，拼搭快乐时光",
      "pointsRequired": 500,
      "images": [
        "https://oss.example.com/gifts/lego1.jpg",
        "https://oss.example.com/gifts/lego2.jpg",
        "https://oss.example.com/gifts/lego3.jpg"
      ],
      "stock": 10,
      "isActive": true
    }
  ]
}
```

### 兑换礼物
```http
POST /exchange
Authorization: Bearer <token>
```

**请求体**
```json
{
  "giftId": 1,
  "contactInfo": "13800138000",
  "deliveryAddress": "北京市朝阳区xxx小区x号楼x单元xxx"
}
```

**响应**
```json
{
  "code": 200,
  "data": {
    "exchangeId": 1234,
    "giftName": "乐高积木玩具",
    "pointsCost": 500,
    "newBalance": 0,
    "status": "pending"
  }
}
```

### 获取兑换记录
```http
GET /exchange/records?page=1
Authorization: Bearer <token>
```

**响应**
```json
{
  "code": 200,
  "data": {
    "list": [
      {
        "id": 1234,
        "giftName": "乐高积木玩具",
        "pointsCost": 500,
        "status": "pending",
        "contactInfo": "138****8000",
        "createdAt": "2024-01-15T12:00:00Z"
      }
    ],
    "total": 3
  }
}
```

---

## 用户统计

### 获取学习统计
```http
GET /users/statistics
Authorization: Bearer <token>
```

**响应**
```json
{
  "code": 200,
  "data": {
    "totalQuestions": 156,
    "correctQuestions": 132,
    "accuracy": 84.6,
    "totalPoints": 370,
    "currentLevel": "学习者",
    "continuousDays": 5,
    "subjectStats": [
      {
        "subject": "math",
        "totalQuestions": 60,
        "correctQuestions": 52,
        "accuracy": 86.7
      },
      {
        "subject": "english",
        "totalQuestions": 50,
        "correctQuestions": 42,
        "accuracy": 84.0
      },
      {
        "subject": "chinese",
        "totalQuestions": 46,
        "correctQuestions": 38,
        "accuracy": 82.6
      }
    ]
  }
}
```

---

## Webhook 通知（可选）

### 每日题目生成完成
```http
POST <webhook_url>
```

**请求体**
```json
{
  "event": "questions_generated",
  "userId": 1,
  "date": "2024-01-15",
  "subjects": ["math", "english", "chinese"],
  "totalQuestions": 15
}
```
