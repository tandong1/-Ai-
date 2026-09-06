# 题目相关API接口文档

## 1. 获取今日题目（未完成时）

**接口**: `GET /api/questions/daily`

**参数**:
- `subject`: 科目（math/english/chinese）

**返回示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "subject": "math",
    "generatedDate": "2026-09-06",
    "questions": [
      {
        "id": 1,
        "questionType": "choice",
        "questionText": "计算: 1/2 + 1/3 = ?",
        "options": ["5/6", "2/5", "1/6", "3/5"],
        "difficulty": "easy",
        "knowledgePoint": "分数加法-通分"
      }
    ]
  }
}
```

**说明**: 
- 题目不含答案和解析
- 只返回未答题的题目
- 如果今日题目已完成或未生成，会返回错误

---

## 2. 提交答案

**接口**: `POST /api/questions/submit`

**请求体**:
```json
{
  "subject": "math",
  "answers": [
    {
      "questionId": 1,
      "userAnswer": "D",
      "isCorrect": false,
      "attemptCount": 3
    },
    {
      "questionId": 2,
      "userAnswer": "10",
      "isCorrect": false,
      "attemptCount": 3
    }
  ]
}
```

**返回示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "subject": "math",
    "totalQuestions": 2,
    "correctCount": 0,
    "pointsEarned": 0,
    "newBalance": 0
  }
}
```

**积分规则**:
- 首次答对：10分
- 二次答对：5分
- 三次及以上答对：2分
- 答错：0分

---

## 3. 获取今日已完成题目详情（做完题后）

**接口**: `GET /api/questions/daily/completed`

**参数**:
- `subject`: 科目（math/english/chinese）

**返回示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": {
    "subject": "math",
    "completedDate": "2026-09-06",
    "questions": [
      {
        "questionId": 1,
        "questionType": "choice",
        "questionText": "计算: 1/2 + 1/3 = ?",
        "options": ["5/6", "2/5", "1/6", "3/5"],
        "userAnswer": "D",
        "correctAnswer": "5/6",
        "isCorrect": false,
        "analysis": "解析：要计算 1/2 + 1/3，首先需要通分...",
        "attemptCount": 3,
        "knowledgePoint": "分数加法-通分"
      }
    ]
  }
}
```

**说明**:
- 显示完整题目信息、用户答案、正确答案和解析
- 用于答题后复习查看
- 如果今日还未完成答题，会返回错误

---

## 4. 获取答题历史记录

**接口**: `GET /api/questions/records`

**参数**（全部可选）:
- `subject`: 科目（math/english/chinese）
- `startDate`: 开始日期（格式：2026-09-01）
- `endDate`: 结束日期（格式：2026-09-06）

**示例**:
- 查询所有历史: `/api/questions/records`
- 查询数学历史: `/api/questions/records?subject=math`
- 查询日期范围: `/api/questions/records?startDate=2026-09-01&endDate=2026-09-06`

**返回示例**:
```json
{
  "code": 200,
  "message": "success",
  "data": [
    {
      "id": 1,
      "questionId": 1,
      "subject": "math",
      "questionType": "choice",
      "questionText": "计算: 1/2 + 1/3 = ?",
      "options": ["5/6", "2/5", "1/6", "3/5"],
      "userAnswer": "D",
      "correctAnswer": "5/6",
      "isCorrect": false,
      "analysis": "解析：要计算 1/2 + 1/3，首先需要通分...",
      "attemptCount": 3,
      "firstAttemptCorrect": false,
      "answeredAt": "2026-09-06T21:32:46"
    }
  ]
}
```

**说明**:
- 按答题时间倒序排列（最新的在前）
- 支持按科目、日期范围筛选
- 返回完整答题信息

---

## 前端使用建议

### 答题流程

1. **进入答题页面**
   - 先调用 `GET /questions/daily/completed` 检查今日是否已完成
   - 如果已完成：显示完成详情（含答案解析）
   - 如果未完成：调用 `GET /questions/daily` 获取题目开始答题

2. **答题过程**
   - 用户在前端答题（可多次尝试）
   - 前端记录每题的尝试次数和最终答案

3. **提交答案**
   - 调用 `POST /questions/submit` 提交所有答案
   - 提交成功后跳转到完成页面，显示积分和正确率

4. **查看历史**
   - 调用 `GET /questions/records` 查询历史记录
   - 可以查看每次答题的详细情况

---

## 错误处理

所有接口在出错时返回：
```json
{
  "code": 500,
  "message": "今日还没有生成题目，请稍后再试",
  "data": null
}
```

常见错误：
- "今日还没有生成题目，请稍后再试"：题目尚未生成
- "今日还没有完成答题"：尝试查看完成详情但还未提交答案
- "用户不存在"：用户ID无效
- "题目不存在"：提交的题目ID无效
