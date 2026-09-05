# Spring Boot 后端项目

## 项目结构说明

```
backend/
├── pom.xml                                    # Maven配置
├── Dockerfile                                 # Docker镜像构建文件
├── src/main/
│   ├── java/com/study/
│   │   ├── StudyApplication.java             # 启动类
│   │   ├── common/                           # 通用类
│   │   │   ├── Result.java                   # ✅ 统一响应结果
│   │   │   ├── BusinessException.java        # ✅ 业务异常
│   │   │   └── GlobalExceptionHandler.java   # ✅ 全局异常处理
│   │   ├── config/                           # 配置类
│   │   │   ├── MyBatisPlusConfig.java        # MyBatis Plus配置
│   │   │   ├── WebConfig.java                # Web配置（CORS等）
│   │   │   └── JwtConfig.java                # JWT配置
│   │   ├── entity/                           # 实体类（数据库映射）
│   │   │   ├── User.java                     # ✅ 用户实体
│   │   │   ├── Question.java                 # ✅ 题目实体
│   │   │   ├── QuestionRecord.java           # ✅ 答题记录实体
│   │   │   ├── WrongQuestion.java            # 错题本实体
│   │   │   ├── PointsRecord.java             # 积分记录实体
│   │   │   ├── Gift.java                     # 礼物实体
│   │   │   ├── ExchangeRecord.java           # 兑换记录实体
│   │   │   ├── KnowledgeBase.java            # 知识库实体
│   │   │   └── Achievement.java              # 成就实体
│   │   ├── dto/                              # 数据传输对象（请求参数）
│   │   │   ├── UserLoginDTO.java             # ✅ 用户登录DTO
│   │   │   ├── SubmitAnswerDTO.java          # ✅ 提交答案DTO
│   │   │   ├── ExchangeGiftDTO.java          # 兑换礼物DTO
│   │   │   └── UploadKnowledgeDTO.java       # 上传知识点DTO
│   │   ├── vo/                               # 视图对象（响应数据）
│   │   │   ├── UserVO.java                   # ✅ 用户信息VO
│   │   │   ├── QuestionVO.java               # ✅ 题目VO
│   │   │   ├── AnswerResultVO.java           # ✅ 答题结果VO
│   │   │   ├── LoginVO.java                  # 登录响应VO
│   │   │   ├── DailyQuestionsVO.java         # 今日题目VO
│   │   │   └── SubmitResultVO.java           # 提交结果VO
│   │   ├── mapper/                           # 持久层接口
│   │   │   ├── UserMapper.java               # ✅ 用户Mapper
│   │   │   ├── QuestionMapper.java           # ✅ 题目Mapper
│   │   │   ├── QuestionRecordMapper.java     # ✅ 答题记录Mapper
│   │   │   ├── WrongQuestionMapper.java      # 错题本Mapper
│   │   │   ├── PointsRecordMapper.java       # 积分记录Mapper
│   │   │   ├── GiftMapper.java               # 礼物Mapper
│   │   │   ├── ExchangeRecordMapper.java     # 兑换记录Mapper
│   │   │   ├── KnowledgeBaseMapper.java      # 知识库Mapper
│   │   │   └── AchievementMapper.java        # 成就Mapper
│   │   ├── service/                          # 业务逻辑层接口
│   │   │   ├── UserService.java              # 用户服务接口
│   │   │   ├── QuestionService.java          # 题目服务接口
│   │   │   ├── PointsService.java            # 积分服务接口
│   │   │   ├── GiftService.java              # 礼物服务接口
│   │   │   ├── KnowledgeBaseService.java     # 知识库服务接口
│   │   │   ├── AiService.java                # AI服务接口
│   │   │   └── impl/                         # 实现类
│   │   │       ├── UserServiceImpl.java      # 用户服务实现
│   │   │       ├── QuestionServiceImpl.java  # 题目服务实现
│   │   │       ├── PointsServiceImpl.java    # 积分服务实现
│   │   │       ├── GiftServiceImpl.java      # 礼物服务实现
│   │   │       ├── KnowledgeBaseServiceImpl.java # 知识库服务实现
│   │   │       └── AiServiceImpl.java        # AI服务实现
│   │   ├── controller/                       # 控制层（API接口）
│   │   │   ├── UserController.java           # 用户控制器
│   │   │   ├── QuestionController.java       # 题目控制器
│   │   │   ├── PointsController.java         # 积分控制器
│   │   │   ├── GiftController.java           # 礼物控制器
│   │   │   ├── KnowledgeController.java      # 知识库控制器
│   │   │   └── HealthController.java         # 健康检查
│   │   ├── scheduler/                        # 定时任务
│   │   │   └── QuestionGeneratorScheduler.java # 题目生成定时任务
│   │   └── util/                             # 工具类
│   │       ├── JwtUtil.java                  # JWT工具类
│   │       └── FileUtil.java                 # 文件工具类
│   └── resources/
│       ├── application.yml                   # ✅ 配置文件
│       └── mapper/                           # MyBatis XML文件（可选）
└── src/test/                                 # 测试代码
    └── java/com/study/
        └── StudyApplicationTests.java
```

## 已创建的文件 ✅

1. **pom.xml** - Maven依赖配置
2. **application.yml** - 应用配置
3. **common/** - 通用类
   - Result.java - 统一响应
   - BusinessException.java - 业务异常
   - GlobalExceptionHandler.java - 全局异常处理
4. **entity/** - 部分实体类
   - User.java
   - Question.java
   - QuestionRecord.java
5. **dto/** - 部分DTO
   - UserLoginDTO.java
   - SubmitAnswerDTO.java
6. **vo/** - 部分VO
   - UserVO.java
   - QuestionVO.java
   - AnswerResultVO.java
7. **mapper/** - 部分Mapper
   - UserMapper.java
   - QuestionMapper.java
   - QuestionRecordMapper.java

## 需要继续创建的文件

由于文件数量较多，我会分批创建。接下来需要创建：

### 优先级1 - 核心功能
1. **Service层** - 用户、题目、积分服务
2. **Controller层** - API接口
3. **启动类** - StudyApplication.java
4. **配置类** - MyBatis Plus、CORS等

### 优先级2 - 完善功能
1. **剩余实体类** - 错题本、积分记录等
2. **AI服务** - 调用大模型API
3. **定时任务** - 每日生成题目
4. **工具类** - JWT、文件处理

### 优先级3 - 增强功能
1. **知识库服务**
2. **礼物商城服务**
3. **成就系统**

## 编译和运行

```bash
# 编译
mvn clean package

# 运行
java -jar target/study-system-1.0.0.jar

# 或使用Maven
mvn spring-boot:run
```

## Docker构建

```bash
# 构建镜像
docker build -t study-backend .

# 运行容器
docker run -p 3000:3000 \
  -e DB_HOST=mysql \
  -e DB_PASSWORD=your_password \
  study-backend
```

## 下一步

我将继续创建：
1. Service接口和实现类
2. Controller控制器
3. 启动类和配置类
4. 工具类

请告诉我是否继续创建，或者有任何调整需求？
