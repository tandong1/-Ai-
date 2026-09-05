# Spring Boot 后端项目 - 完整代码说明

## 项目信息

- **框架**: Spring Boot 3.2.1
- **Java版本**: 17
- **构建工具**: Maven
- **数据库**: MySQL 8.0
- **ORM框架**: MyBatis Plus 3.5.5

## 已完成的功能模块

### ✅ 1. 基础架构

#### 通用组件 (common/)
- `Result.java` - 统一响应格式
- `BusinessException.java` - 业务异常类
- `GlobalExceptionHandler.java` - 全局异常处理

#### 配置类 (config/)
- `MyBatisPlusConfig.java` - MyBatis Plus配置（分页、自动填充）
- `WebConfig.java` - Web配置（CORS、拦截器）
- `JwtInterceptor.java` - JWT拦截器

#### 工具类 (util/)
- `JwtUtil.java` - JWT生成和验证

### ✅ 2. 用户模块

#### 实体 (entity/)
- `User.java` - 用户实体

#### DTO (dto/)
- `UserLoginDTO.java` - 登录请求

#### VO (vo/)
- `UserVO.java` - 用户信息响应
- `LoginVO.java` - 登录响应

#### 持久层 (mapper/)
- `UserMapper.java` - 用户数据访问

#### 业务层 (service/)
- `UserService.java` - 用户服务接口
- `UserServiceImpl.java` - 用户服务实现
  - 用户列表查询
  - 用户登录（生成JWT）
  - 获取用户信息
  - 更新用户积分

#### 控制层 (controller/)
- `UserController.java` - 用户API
  - `GET /api/users` - 获取用户列表
  - `POST /api/users/login` - 用户登录

### ✅ 3. 题目模块

#### 实体 (entity/)
- `Question.java` - 题目实体（含答案和解析）
- `QuestionRecord.java` - 答题记录实体

#### DTO (dto/)
- `SubmitAnswerDTO.java` - 提交答案请求

#### VO (vo/)
- `QuestionVO.java` - 题目响应（不含答案）
- `DailyQuestionsVO.java` - 今日题目列表
- `AnswerResultVO.java` - 答题结果
- `SubmitResultVO.java` - 提交结果统计

#### 持久层 (mapper/)
- `QuestionMapper.java` - 题目数据访问
- `QuestionRecordMapper.java` - 答题记录数据访问

#### 业务层 (service/)
- `QuestionService.java` - 题目服务接口
- `QuestionServiceImpl.java` - 题目服务实现
  - 获取今日题目（不含答案和解析）
  - 提交答案并批改
  - 自动计算积分
  - 保存答题记录

#### 控制层 (controller/)
- `QuestionController.java` - 题目API
  - `GET /api/questions/daily?subject=math` - 获取今日题目
  - `POST /api/questions/submit` - 提交答案

### ✅ 4. 健康检查
- `HealthController.java` - 健康检查
  - `GET /api/health` - 健康检查接口

### ✅ 5. 启动类
- `StudyApplication.java` - Spring Boot启动类

### ✅ 6. Docker支持
- `Dockerfile` - 多阶段构建配置

## API接口清单

### 用户相关

```bash
# 1. 获取用户列表（无需认证）
GET /api/users

# 2. 用户登录
POST /api/users/login
Content-Type: application/json
{
  "userId": 1
}

# 响应
{
  "code": 200,
  "message": "success",
  "data": {
    "token": "eyJhbGciOiJIUzI1NiIs...",
    "user": {
      "id": 1,
      "name": "大宝",
      "avatar": "/images/avatar1.png",
      "totalPoints": 0,
      "currentLevel": "初学者"
    }
  }
}
```

### 题目相关

```bash
# 1. 获取今日题目（需要认证）
GET /api/questions/daily?subject=math
Authorization: Bearer <token>

# 响应
{
  "code": 200,
  "data": {
    "subject": "math",
    "generatedDate": "2024-01-15",
    "questions": [
      {
        "id": 1,
        "questionType": "choice",
        "questionText": "计算: 1/2 + 1/3 = ?",
        "options": ["5/6", "2/5", "1/6", "3/5"],
        "difficulty": "easy",
        "knowledgePoint": "分数加法"
      }
    ]
  }
}

# 2. 提交答案（需要认证）
POST /api/questions/submit
Authorization: Bearer <token>
Content-Type: application/json
{
  "subject": "math",
  "answers": [
    {
      "questionId": 1,
      "userAnswer": "5/6"
    }
  ]
}

# 响应
{
  "code": 200,
  "data": {
    "results": [
      {
        "questionId": 1,
        "isCorrect": true,
        "userAnswer": "5/6",
        "correctAnswer": "5/6",
        "analysis": "解析：要计算 1/2 + 1/3，首先需要通分...",
        "pointsEarned": 10
      }
    ],
    "totalPointsEarned": 10,
    "correctCount": 1,
    "totalCount": 1,
    "accuracy": 100.0,
    "newBalance": 10
  }
}
```

### 健康检查

```bash
GET /api/health

# 响应
{
  "code": 200,
  "data": {
    "status": "UP",
    "timestamp": 1705305600000
  }
}
```

## 项目运行

### 本地开发

```bash
# 1. 配置环境变量（或修改application.yml）
export DB_HOST=localhost
export DB_PORT=3306
export DB_NAME=study_system
export DB_USER=study_user
export DB_PASSWORD=your_password
export JWT_SECRET=your_jwt_secret_at_least_32_characters_long

# 2. 运行数据库初始化脚本
mysql -u study_user -p study_system < ../../docs/init.sql

# 3. 启动应用
mvn spring-boot:run

# 或者先编译再运行
mvn clean package
java -jar target/study-system-1.0.0.jar
```

### Docker部署

```bash
# 1. 构建镜像
docker build -t study-backend .

# 2. 运行容器
docker run -d \
  -p 3000:3000 \
  -e DB_HOST=mysql \
  -e DB_PASSWORD=your_password \
  -e JWT_SECRET=your_jwt_secret \
  --name study-backend \
  study-backend

# 3. 查看日志
docker logs -f study-backend
```

### 使用docker-compose

```bash
# 在项目根目录执行
docker-compose up -d

# 查看日志
docker-compose logs -f backend
```

## 代码结构说明

### MVC三层架构

```
Controller层 (接收请求、参数校验)
    ↓
Service层 (业务逻辑处理)
    ↓
Mapper层 (数据库操作)
    ↓
Database (MySQL)
```

### 数据流转

```
1. 客户端发起请求 → Controller
2. Controller接收参数 → 转换为DTO
3. Controller调用Service → 传入DTO
4. Service处理业务逻辑 → 调用Mapper
5. Mapper执行SQL → 返回Entity
6. Service转换Entity → 返回VO
7. Controller包装Result → 返回给客户端
```

### 认证流程

```
1. 用户登录 → 生成JWT token
2. 客户端请求 → 携带token在Header中
3. JwtInterceptor拦截 → 验证token
4. 验证通过 → 提取userId放入request
5. Controller获取userId → @RequestAttribute("userId")
6. Service使用userId → 处理业务
```

## 注意事项

### 1. JWT配置
- `jwt.secret` 必须至少32个字符
- `jwt.expiration` 单位是毫秒（默认7天）

### 2. 数据库连接
- 确保MySQL时区设置正确：`serverTimezone=Asia/Shanghai`
- 字符编码使用UTF-8：`characterEncoding=utf8`

### 3. 跨域配置
- 已配置允许所有来源（开发环境）
- 生产环境建议指定具体域名

### 4. 日志配置
- 日志文件位置：`logs/study-system.log`
- 最大单文件：100MB
- 保留天数：30天

## 后续需要实现的功能

1. **积分记录模块** - 积分明细查询
2. **错题本模块** - 错题列表、标记掌握
3. **礼物商城模块** - 礼物列表、兑换功能
4. **知识库模块** - 上传知识点、AI提取
5. **AI服务模块** - 调用大模型生成题目
6. **定时任务** - 每日凌晨自动生成题目
7. **成就系统** - 成就解锁、查询

## 测试建议

### 1. 单元测试
```bash
mvn test
```

### 2. 集成测试
使用Postman或curl测试API

### 3. 压力测试
使用JMeter或ab工具

---

**项目已完成核心架构和用户、题目两大核心模块，严格遵循MVC三层架构！**
